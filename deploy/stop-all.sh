#!/usr/bin/env bash
#
# 停止三件套：前端 → 后端 → MySQL
#
set -uo pipefail

BASE="${SCHOOLMATE_BASE:-$HOME/schoolmate_book}"
ENV_FILE="$BASE/app/env.sh"
[ -f "$ENV_FILE" ] && . "$ENV_FILE"

export LD_LIBRARY_PATH="$BASE/runtime/lib:${LD_LIBRARY_PATH:-}"

MYSQL_HOME="$BASE/runtime/mysql"
MYCNF="$BASE/app/my.cnf"
PIDDIR="$BASE/pids"

kill_pid_file() {
  local name="$1" pid_file="$2"
  if [ -f "$pid_file" ]; then
    local pid
    pid="$(cat "$pid_file")"
    if kill -0 "$pid" 2>/dev/null; then
      echo "[$name] 停止 pid $pid"
      kill "$pid" 2>/dev/null
      for i in $(seq 1 15); do kill -0 "$pid" 2>/dev/null || break; sleep 1; done
      kill -0 "$pid" 2>/dev/null && kill -9 "$pid" 2>/dev/null
    else
      echo "[$name] 未在运行"
    fi
    rm -f "$pid_file"
  else
    echo "[$name] 无 pid 文件"
  fi
}

# 前端 vite preview：pid 文件是 vite 进程本身
kill_pid_file "前端" "$PIDDIR/frontend.pid"
# 兜底：清掉可能残留的 vite preview
pkill -f "vite.js preview" 2>/dev/null || true

kill_pid_file "后端" "$PIDDIR/backend.pid"

if [ -f "$MYCNF" ]; then
  if "$MYSQL_HOME/bin/mysqladmin" --defaults-file="$MYCNF" -u root -p"$DB_ROOT_PASSWORD" ping >/dev/null 2>&1; then
    echo "[MySQL] 关闭中 ..."
    "$MYSQL_HOME/bin/mysqladmin" --defaults-file="$MYCNF" -u root -p"$DB_ROOT_PASSWORD" shutdown
  else
    echo "[MySQL] 未在运行"
  fi
fi

echo "已停止。"
