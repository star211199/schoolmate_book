#!/usr/bin/env bash
#
# 重置演示数据库：删库重建 + 重新导入种子 SQL + 清掉测试残留。
# 用途：把服务器上的示例库恢复到「干净可展示」的状态。
#
# 注意：脚本会删除 SQL 种子里的示例班级（class_info / class_member），
#       目的是让班级通过「建班接口」重建 —— 只有走接口建班，才会自动创建
#       对应的班级群与群会话（ensureClassGroup）。种子里直接用 SQL 插入的班级
#       没有群，聊天页看不到班级群。
#       因此执行完本脚本后，还需要调用一次 POST /api/classes 把班级建回来。
#
set -euo pipefail

BASE="${SCHOOLMATE_BASE:-$HOME/schoolmate_book}"
. "$BASE/app/env.sh"

# MySQL 8.0.29 依赖 libaio.so.1（Ubuntu 24.04 只提供 libaio.so.1t64）
export LD_LIBRARY_PATH="$BASE/runtime/lib:${LD_LIBRARY_PATH:-}"

MYSQL_HOME="$BASE/runtime/mysql"
MYCNF="$BASE/app/my.cnf"
M=("$MYSQL_HOME/bin/mysql" "--defaults-file=$MYCNF" -u root -p"$DB_ROOT_PASSWORD")

echo "==> 停止后端（避免连接池持有旧连接）"
PID_FILE="$BASE/pids/backend.pid"
if [ -f "$PID_FILE" ] && kill -0 "$(cat "$PID_FILE")" 2>/dev/null; then
  kill "$(cat "$PID_FILE")" 2>/dev/null || true
  for _ in $(seq 1 15); do kill -0 "$(cat "$PID_FILE")" 2>/dev/null || break; sleep 1; done
  kill -9 "$(cat "$PID_FILE")" 2>/dev/null || true
  rm -f "$PID_FILE"
  echo "    后端已停止"
else
  echo "    后端未在运行"
fi

echo "==> 重建数据库 $DB_NAME"
"${M[@]}" -e "DROP DATABASE IF EXISTS \`$DB_NAME\`;
              CREATE DATABASE \`$DB_NAME\` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;"

for f in init.sql upgrade.sql v2-init.sql upgrade-v3.sql; do
  echo "==> 导入 $f"
  "${M[@]}" "$DB_NAME" < "$BASE/app/sql/$f" 2>&1 | grep -viE "already exists|Duplicate" || true
done

echo "==> 删除 SQL 种子班级，并把自增列归零（稍后通过接口重建）"
"${M[@]}" "$DB_NAME" -e "
  DELETE FROM class_member;
  DELETE FROM class_info;
  ALTER TABLE class_info   AUTO_INCREMENT = 1;
  ALTER TABLE class_member AUTO_INCREMENT = 1;
  ALTER TABLE chat_group   AUTO_INCREMENT = 1;
  ALTER TABLE chat_session AUTO_INCREMENT = 1;
"

echo "==> 残留数据检查（应当全部为 0）"
"${M[@]}" "$DB_NAME" -N -B -e "
  SELECT 'class_info',    COUNT(*) FROM class_info
  UNION ALL SELECT 'class_member',  COUNT(*) FROM class_member
  UNION ALL SELECT 'chat_group',    COUNT(*) FROM chat_group
  UNION ALL SELECT 'chat_message',  COUNT(*) FROM chat_message
  UNION ALL SELECT 'friendship',    COUNT(*) FROM friendship
  UNION ALL SELECT 'friend_request',COUNT(*) FROM friend_request
  UNION ALL SELECT 'moment',        COUNT(*) FROM moment
  UNION ALL SELECT 'notification',  COUNT(*) FROM notification
  UNION ALL SELECT 'time_capsule',  COUNT(*) FROM time_capsule
  UNION ALL SELECT 'user',          COUNT(*) FROM user;"

echo "==> 表数量：$("${M[@]}" -N -e "select count(*) from information_schema.tables where table_schema='$DB_NAME'")"
echo
echo "下一步："
echo "  bash $BASE/deploy/start-all.sh"
echo "  然后调用 POST /api/classes 重建班级（脚本 deploy/seed-demo.sh 会自动完成）"
