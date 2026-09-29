#!/usr/bin/env bash
#
# 一次性初始化服务器：生成配置与密钥、初始化 MySQL 数据目录、建库导入 SQL。
# 免 sudo：所有文件都放在 $BASE（默认 ~/schoolmate_book）下。
# 幂等：重复执行不会覆盖已有的 env.sh / 数据目录。
#
set -euo pipefail

BASE="${SCHOOLMATE_BASE:-$HOME/schoolmate_book}"
JDK="$BASE/runtime/jdk"
MYSQL_HOME="$BASE/runtime/mysql"
MYSQL_DATA="$BASE/data/mysql"
MYSQL_SOCK="$BASE/data/mysql.sock"
APP="$BASE/app"
WEB="$BASE/web"
LOG="$BASE/logs"

echo "==> BASE = $BASE"
mkdir -p "$BASE"/{runtime,app,web,logs,data,downloads} "$APP/upload" "$LOG"

[ -x "$JDK/bin/java" ] || { echo "!! 未找到 JDK：$JDK/bin/java"; exit 1; }
[ -x "$MYSQL_HOME/bin/mysqld" ] || { echo "!! 未找到 MySQL：$MYSQL_HOME/bin/mysqld"; exit 1; }

# ---------- 0. 动态库兼容层 ----------
# Ubuntu 24.04 的两个改名/升级导致 MySQL 8.0.29 的预编译二进制找不到依赖：
#   1) libaio 包改名为 libaio1t64，库文件变成 libaio.so.1t64，而 mysqld 找 libaio.so.1
#   2) mysql 命令行客户端链接的是 libtinfo.so.5，而系统只提供 libtinfo.so.6
# 无 sudo 改不了系统库，因此做软链接 + LD_LIBRARY_PATH 指向本地目录。
mkdir -p "$BASE/runtime/lib"

LIBAIO_SRC="/usr/lib/x86_64-linux-gnu/libaio.so.1t64"
[ -e "$LIBAIO_SRC" ] || LIBAIO_SRC="/usr/lib/x86_64-linux-gnu/libaio.so.1"
if [ -e "$LIBAIO_SRC" ]; then
  ln -sf "$LIBAIO_SRC" "$BASE/runtime/lib/libaio.so.1"
  echo "==> libaio 兼容层：libaio.so.1 -> $LIBAIO_SRC"
fi

LibtinfoSrc=""
for p in /lib/x86_64-linux-gnu/libtinfo.so.6 /usr/lib/x86_64-linux-gnu/libtinfo.so.6; do
  [ -e "$p" ] && LibtinfoSrc="$p" && break
done
if [ -n "$LibtinfoSrc" ] && [ ! -e "$BASE/runtime/lib/libtinfo.so.5" ]; then
  ln -sf "$LibtinfoSrc" "$BASE/runtime/lib/libtinfo.so.5"
  echo "==> libtinfo 兼容层：libtinfo.so.5 -> $LibtinfoSrc"
fi

export LD_LIBRARY_PATH="$BASE/runtime/lib:${LD_LIBRARY_PATH:-}"

# ---------- 1. 生成密钥与数据库密码（已存在则复用） ----------
ENV_FILE="$APP/env.sh"
if [ ! -f "$ENV_FILE" ]; then
  echo "==> 生成 env.sh（含随机密钥，权限 600）"
  DB_ROOT_PASSWORD="$(head -c 24 /dev/urandom | base64 | tr -d '/+=' | head -c 24)"
  DB_PASSWORD="$(head -c 24 /dev/urandom | base64 | tr -d '/+=' | head -c 24)"
  JWT_SECRET="$(head -c 48 /dev/urandom | base64 | tr -d '/+=' | head -c 64)"
  cat > "$ENV_FILE" <<ENVEOF
# 便携部署环境变量（请勿提交到版本库）
export SCHOOLMATE_BASE="$BASE"
export SERVER_PORT=8080
export DB_HOST=127.0.0.1
export DB_PORT=3306
export DB_NAME=schoolmate_book
export DB_USERNAME=schoolmate
export DB_PASSWORD="$DB_PASSWORD"
export DB_ROOT_PASSWORD="$DB_ROOT_PASSWORD"
export JWT_SECRET="$JWT_SECRET"
export JWT_EXPIRE_MINUTES=1440
export SWAGGER_ENABLED=false
ENVEOF
  chmod 600 "$ENV_FILE"
else
  echo "==> env.sh 已存在，复用"
fi
# shellcheck disable=SC1090
. "$ENV_FILE"

# ---------- 2. 生成 my.cnf ----------
MYCNF="$APP/my.cnf"
if [ ! -f "$MYCNF" ]; then
  echo "==> 生成 my.cnf"
  cat > "$MYCNF" <<CNFEOF
[mysqld]
basedir=$MYSQL_HOME
datadir=$MYSQL_DATA
socket=$MYSQL_SOCK
pid-file=$BASE/data/mysql.pid
port=$DB_PORT
bind-address=127.0.0.1
skip-name-resolve
character-set-server=utf8mb4
collation-server=utf8mb4_general_ci
default-time-zone=+08:00
log-error=$LOG/mysql-error.log
max_connections=200
innodb_buffer_pool_size=512M
[client]
socket=$MYSQL_SOCK
CNFEOF
fi

# ---------- 3. 初始化 MySQL 数据目录 ----------
if [ ! -d "$MYSQL_DATA/mysql" ]; then
  echo "==> 初始化 MySQL 数据目录（root 无密码）"
  mkdir -p "$MYSQL_DATA"
  "$MYSQL_HOME/bin/mysqld" --defaults-file="$MYCNF" --initialize-insecure
  echo "    初始化完成"
else
  echo "==> MySQL 数据目录已存在，跳过初始化"
fi

# ---------- 4. 启动 MySQL ----------
if "$MYSQL_HOME/bin/mysqladmin" --defaults-file="$MYCNF" -u root ping >/dev/null 2>&1; then
  echo "==> MySQL 已在运行"
else
  echo "==> 启动 MySQL"
  "$MYSQL_HOME/bin/mysqld" --defaults-file="$MYCNF" --daemonize
  for i in $(seq 1 40); do
    "$MYSQL_HOME/bin/mysqladmin" --defaults-file="$MYCNF" -u root ping >/dev/null 2>&1 && break
    sleep 1
  done
fi
"$MYSQL_HOME/bin/mysqladmin" --defaults-file="$MYCNF" -u root ping >/dev/null 2>&1 \
  || { echo "!! MySQL 启动失败，见 $LOG/mysql-error.log"; exit 1; }
echo "    MySQL 已就绪"

# ---------- 5. 设置 root 密码并建业务账号 ----------
MYSQL_CLI=("$MYSQL_HOME/bin/mysql" --defaults-file="$MYCNF" -u root)
if ! "${MYSQL_CLI[@]}" -p"$DB_ROOT_PASSWORD" -e "select 1" >/dev/null 2>&1; then
  echo "==> 设置 root 密码并创建业务账号 $DB_USERNAME"
  "${MYSQL_CLI[@]}" <<SQLEOF
ALTER USER 'root'@'localhost' IDENTIFIED BY '$DB_ROOT_PASSWORD';
CREATE USER IF NOT EXISTS '$DB_USERNAME'@'127.0.0.1' IDENTIFIED BY '$DB_PASSWORD';
CREATE USER IF NOT EXISTS '$DB_USERNAME'@'localhost' IDENTIFIED BY '$DB_PASSWORD';
GRANT ALL PRIVILEGES ON \`$DB_NAME\`.* TO '$DB_USERNAME'@'127.0.0.1';
GRANT ALL PRIVILEGES ON \`$DB_NAME\`.* TO '$DB_USERNAME'@'localhost';
FLUSH PRIVILEGES;
SQLEOF
  MYSQL_CLI=("$MYSQL_HOME/bin/mysql" --defaults-file="$MYCNF" -u root -p"$DB_ROOT_PASSWORD")
fi

# ---------- 6. 导入 SQL（幂等：已存在的表不会被重建） ----------
MYSQL_CLI=("$MYSQL_HOME/bin/mysql" --defaults-file="$MYCNF" -u root -p"$DB_ROOT_PASSWORD")
for f in "$APP"/sql/init.sql "$APP"/sql/upgrade.sql "$APP"/sql/v2-init.sql; do
  [ -f "$f" ] || { echo "    (跳过不存在的 $f)"; continue; }
  echo "==> 导入 $(basename "$f")"
  "${MYSQL_CLI[@]}" < "$f" 2>&1 | grep -viE "already exists|Duplicate" || true
done

echo "==> 校验表数量"
"${MYSQL_CLI[@]}" -N -e "select count(*) from information_schema.tables where table_schema='$DB_NAME';"

echo
echo "初始化完成。密钥保存在 $ENV_FILE（权限 600）"
echo "下一步（二选一）："
echo "  ① 本地开发：把前端产物放到 $WEB 后执行  bash $BASE/deploy/start-all.sh"
echo "  ② 已上传产物：直接执行                   bash $BASE/deploy/start-all.sh"
