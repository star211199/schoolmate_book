#!/usr/bin/env bash
# 在服务器上原地更新「后端 jar / 前端 dist」并重启服务。
#
# 用法：bash deploy/redeploy.sh [stage_dir]     # 默认 _stage3
#
# 为什么不直接用一行 ssh 命令做这件事：
#   pkill -f "schoolmate-server.jar" 会匹配到调用它的那个进程 —— 如果这条命令由 ssh
#   直接执行，而命令文本本身含有该字符串（比如同一行里有 cp ... schoolmate-server.jar），
#   就会把执行者自己杀掉，部署半路中断（本次就是这么踩到的）。
#   写成脚本文件后，脚本进程的命令行只有 "bash deploy/redeploy.sh"，不会再自伤；
#   pkill 自己那份带方括号的模式也不会匹配自身。
set -euo pipefail

BASE="$HOME/schoolmate_book"
STAGE="${1:-$BASE/_stage3}"
cd "$BASE"

[ -d "$STAGE" ] || { echo "暂存目录不存在：$STAGE"; exit 1; }

kill_by_pidfile() {
  local f="pids/$1.pid"
  if [ -f "$f" ]; then kill "$(cat "$f")" 2>/dev/null || true; fi
}

echo "== 1. 停止前端与后端 =="
kill_by_pidfile frontend
kill_by_pidfile backend
pkill -f "[v]ite.js preview" 2>/dev/null || true
pkill -f "[s]choolmate-server.jar" 2>/dev/null || true
sleep 3
if pgrep -f "[s]choolmate-server.jar" >/dev/null 2>&1; then
  echo "  后端未退出，强制结束"
  pkill -9 -f "[s]choolmate-server.jar" 2>/dev/null || true
  sleep 2
fi

echo "== 2. 替换后端 jar =="
if [ -f "$STAGE/schoolmate-server.jar" ]; then
  cp "$STAGE/schoolmate-server.jar" app/schoolmate-server.jar
  echo "  已替换，md5=$(md5sum app/schoolmate-server.jar | cut -c1-12)"
else
  echo "  暂存目录无 jar，跳过"
fi

echo "== 3. 替换前端 dist =="
PKG=$(ls "$STAGE"/dist*.tar.gz 2>/dev/null | head -1 || true)
if [ -n "$PKG" ]; then
  rm -rf web/dist && mkdir -p web/dist
  tar xzf "$PKG" -C web/dist
  echo "  已解包 $(basename "$PKG")，大小=$(du -sh web/dist | cut -f1)，首页引用=$(grep -o 'index-[A-Za-z0-9_-]*\.js' web/dist/index.html | head -1)"
else
  echo "  暂存目录无 dist 包，跳过"
fi

echo "== 4. 启动全部服务 =="
bash deploy/start-all.sh

echo "== 5. 进程状态 =="
for f in pids/*.pid; do
  [ -f "$f" ] || continue
  pid=$(cat "$f")
  if kill -0 "$pid" 2>/dev/null; then
    echo "  $(basename "$f") = $pid 运行中"
  else
    echo "  $(basename "$f") = $pid 已退出（异常！）"
  fi
done
