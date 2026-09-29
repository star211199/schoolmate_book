#!/usr/bin/env bash
#
# 公网入口（内网穿透）。用法：
#   bash start-tunnel.sh natapp <authtoken>   # natapp（推荐，隧道已配置映射本地 5173）
#   bash start-tunnel.sh install-natapp <authtoken>  # 仅安装/升级 natapp 客户端
#   bash start-tunnel.sh cloudflared          # 无需账号，出临时 URL（每次重启都会变）
#   bash start-tunnel.sh stop                 # 停止隧道
#
# 关于 natapp：
#   natapp v3 的「本地端口」在云端隧道配置里设置，命令行没有 -localport 参数。
#   本项目要求隧道映射 5173（前端 vite preview 的监听端口）。
#
set -uo pipefail

BASE="${SCHOOLMATE_BASE:-$HOME/schoolmate_book}"
LOG="$BASE/logs"
PIDDIR="$BASE/pids"
DOWNLOADS="$BASE/downloads"
NATAPP_DIR="$DOWNLOADS/natapp_bin"     # 官方脚本的安装目录（可用 NATAPP_INSTALL_DIR 覆盖）
NATAPP_BIN="$NATAPP_DIR/natapp"
mkdir -p "$LOG" "$PIDDIR" "$DOWNLOADS"

MODE="${1:-natapp}"

stop_existing() {
  for n in cloudflared natapp; do
    if [ -f "$PIDDIR/$n.pid" ]; then
      kill "$(cat "$PIDDIR/$n.pid")" 2>/dev/null || true
      rm -f "$PIDDIR/$n.pid"
    fi
  done
  # 用方括号避免 pkill 匹配到本脚本自身的命令行（否则会把自己杀掉）
  pkill -f "[c]loudflared tunnel" 2>/dev/null || true
  pkill -f "[n]atapp -authtoken" 2>/dev/null || true
  sleep 1
}

install_natapp() {
  local token="$1"
  [ -n "$token" ] || { echo "用法：bash start-tunnel.sh install-natapp <authtoken>"; exit 1; }
  echo "==> 通过官网一键脚本安装 natapp 到 $NATAPP_DIR"
  echo "    （官方脚本免 sudo，只写入该目录；下载地址由服务端烘焙 authtoken 下发）"
  NATAPP_INSTALL_DIR="$NATAPP_DIR" \
    curl -fsSL "https://natapp.cn/get.sh?authtoken=$token" | sh
  [ -x "$NATAPP_BIN" ] || { echo "!! 安装后仍未找到 $NATAPP_BIN"; exit 1; }
}

case "$MODE" in
  install-natapp)
    install_natapp "${2:-}"
    ;;

  natapp)
    TOKEN="${2:-}"
    if [ -z "$TOKEN" ]; then
      echo "用法：bash start-tunnel.sh natapp <authtoken>"; exit 1
    fi
    # 未安装则自动安装（v3.0.5 起可直接下载，早期版本只能官网手动取）
    if [ ! -x "$NATAPP_BIN" ]; then
      echo "==> 未检测到 natapp，先自动安装"
      install_natapp "$TOKEN"
    fi

    stop_existing
    echo "==> 启动 natapp 隧道（本地端口由云端隧道配置决定，本项目需为 5173）"
    cd "$NATAPP_DIR"
    nohup "$NATAPP_BIN" -authtoken="$TOKEN" -log=stdout > "$LOG/natapp.log" 2>&1 &
    echo $! > "$PIDDIR/natapp.pid"

    URL=""
    for i in $(seq 1 20); do
      URL=$(grep -oE "Tunnel established at https?://[a-zA-Z0-9.-]+" "$LOG/natapp.log" 2>/dev/null \
            | head -1 | sed 's/.*at //')
      [ -n "$URL" ] && break
      sleep 1
    done

    LOCAL=$(grep -oE "local=127\.0\.0\.1:[0-9]+" "$LOG/natapp.log" 2>/dev/null | tail -1 | cut -d: -f2)
    if [ -n "$URL" ]; then
      echo
      echo "公网地址： $URL"
      echo "转发目标： 127.0.0.1:${LOCAL:-?}"
      if [ "${LOCAL:-}" != "5173" ]; then
        echo
        echo "!! 警告：隧道转发到了 ${LOCAL:-未知} 端口，而不是 5173。"
        echo "   请到 natapp.cn →「隧道列表」把该隧道的本地端口改成 5173，否则页面打不开。"
      fi
      echo
      echo "提示：免费隧道域名/端口会被不定时强制更换；需要固定域名请购买 VIP 隧道。"
    else
      echo "!! 未取到 URL，请查看 $LOG/natapp.log"
    fi
    ;;

  cloudflared)
    BIN="$DOWNLOADS/cloudflared"
    [ -x "$BIN" ] || { echo "!! 缺少 $BIN，请先下载 cloudflared"; exit 1; }
    stop_existing
    echo "==> 启动 cloudflared 快速隧道 → 127.0.0.1:5173"
    nohup "$BIN" tunnel --url http://127.0.0.1:5173 --no-autoupdate \
      > "$LOG/cloudflared.log" 2>&1 &
    echo $! > "$PIDDIR/cloudflared.pid"
    URL=""
    for i in $(seq 1 30); do
      URL=$(grep -oE "https://[a-z0-9-]+\.trycloudflare\.com" "$LOG/cloudflared.log" 2>/dev/null | head -1)
      [ -n "$URL" ] && break
      sleep 1
    done
    if [ -n "$URL" ]; then
      echo
      echo "公网地址（HTTP）： ${URL/http:\/\//http://}"
      echo "公网地址（HTTPS）：$URL"
      echo
      echo "提示：部分网络下 HTTPS 会因 TLS 层拦截而不可用，这种情况下改用 HTTP 访问即可。"
      echo "     该地址每次重启隧道都会变化，需要固定域名请改用 natapp 或 Cloudflare 具名隧道。"
    else
      echo "!! 未取到 URL，请查看 $LOG/cloudflared.log"
    fi
    ;;

  stop)
    stop_existing
    echo "隧道已停止"
    ;;

  *)
    echo "用法：bash start-tunnel.sh [natapp <authtoken> | install-natapp <authtoken> | cloudflared | stop]"
    exit 1
    ;;
esac
