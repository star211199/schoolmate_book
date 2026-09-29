#!/usr/bin/env bash
#
# 启动三件套：便携 MySQL → 后端 jar → 前端静态托管（vite preview，单端口代理 /api 与 WebSocket）
# 幂等：已在运行的组件会被跳过。
#
set -uo pipefail

BASE="${SCHOOLMATE_BASE:-$HOME/schoolmate_book}"
ENV_FILE="$BASE/app/env.sh"
[ -f "$ENV_FILE" ] && . "$ENV_FILE"

# MySQL 8.0.29 依赖 libaio.so.1，而 Ubuntu 24.04 只提供 libaio.so.1t64，
# 用本地软链接目录补上（无 sudo 方案）。
export LD_LIBRARY_PATH="$BASE/runtime/lib:${LD_LIBRARY_PATH:-}"

JDK="$BASE/runtime/jdk"
MYSQL_HOME="$BASE/runtime/mysql"
MYCNF="$BASE/app/my.cnf"
APP="$BASE/app"
WEB="$BASE/web"
LOG="$BASE/logs"
PIDDIR="$BASE/pids"
mkdir -p "$LOG" "$PIDDIR" "$APP/upload"

start_mysql() {
  if "$MYSQL_HOME/bin/mysqladmin" --defaults-file="$MYCNF" -u root -p"$DB_ROOT_PASSWORD" ping >/dev/null 2>&1; then
    echo "[MySQL] 已在运行"; return 0
  fi
  echo "[MySQL] 启动中 ..."
  "$MYSQL_HOME/bin/mysqld" --defaults-file="$MYCNF" --daemonize
  for i in $(seq 1 40); do
    "$MYSQL_HOME/bin/mysqladmin" --defaults-file="$MYCNF" -u root -p"$DB_ROOT_PASSWORD" ping >/dev/null 2>&1 && break
    sleep 1
  done
  if "$MYSQL_HOME/bin/mysqladmin" --defaults-file="$MYCNF" -u root -p"$DB_ROOT_PASSWORD" ping >/dev/null 2>&1; then
    echo "[MySQL] 就绪"
  else
    echo "[MySQL] 启动失败，见 $LOG/mysql-error.log"; return 1
  fi
}

start_backend() {
  local pid_file="$PIDDIR/backend.pid"
  if [ -f "$pid_file" ] && kill -0 "$(cat "$pid_file")" 2>/dev/null; then
    echo "[后端] 已在运行 (pid $(cat "$pid_file"))"; return 0
  fi
  echo "[后端] 启动中 ..."
  cd "$APP"
  nohup "$JDK/bin/java" -Xms256m -Xmx1024m \
    -Dfile.encoding=UTF-8 \
    -jar "$APP/schoolmate-server.jar" \
    --spring.profiles.active=prod \
    --spring.config.additional-location="file:$APP/" \
    --schoolmate.upload.base-dir="$APP/upload" \
    >> "$LOG/backend.log" 2>&1 &
  echo $! > "$pid_file"
  for i in $(seq 1 60); do
    if curl -s -o /dev/null --max-time 2 "http://127.0.0.1:${SERVER_PORT:-8080}/api/auth/login"; then break; fi
    sleep 1
  done
  if kill -0 "$(cat "$pid_file")" 2>/dev/null; then
    echo "[后端] 已启动 (pid $(cat "$pid_file"))，日志 $LOG/backend.log"
  else
    echo "[后端] 启动失败，见 $LOG/backend.log"; return 1
  fi
}

start_frontend() {
  local pid_file="$PIDDIR/frontend.pid"
  if [ -f "$pid_file" ] && kill -0 "$(cat "$pid_file")" 2>/dev/null; then
    echo "[前端] 已在运行 (pid $(cat "$pid_file"))"; return 0
  fi
  if [ ! -f "$WEB/dist/index.html" ]; then
    echo "[前端] 缺少 $WEB/dist/index.html，请先上传前端产物"; return 1
  fi
  if [ ! -d "$WEB/node_modules/vite" ]; then
    echo "[前端] 缺少 vite 依赖，请在 $WEB 执行 npm ci"; return 1
  fi
  echo "[前端] 启动中 ..."
  cd "$WEB"
  nohup node "$WEB/node_modules/vite/bin/vite.js" preview --host 127.0.0.1 --port 5173 \
    >> "$LOG/frontend.log" 2>&1 &
  echo $! > "$pid_file"
  for i in $(seq 1 30); do
    curl -s -o /dev/null --max-time 2 "http://127.0.0.1:5173/" && break
    sleep 1
  done
  if kill -0 "$(cat "$pid_file")" 2>/dev/null; then
    echo "[前端] 已启动 (pid $(cat "$pid_file"))，日志 $LOG/frontend.log"
  else
    echo "[前端] 启动失败，见 $LOG/frontend.log"; return 1
  fi
}

start_mysql
start_backend
start_frontend

echo
echo "访问地址（服务器本机）： http://127.0.0.1:5173"
echo "后端接口：                http://127.0.0.1:${SERVER_PORT:-8080}/api"
echo "查看日志：                tail -f $LOG/backend.log $LOG/frontend.log"
