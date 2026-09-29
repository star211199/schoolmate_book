/**
 * 聊天 WebSocket 客户端。
 *
 * 设计要点（这几点决定了聊天稳不稳，缺一不可）：
 *  1. 单例连接：整个应用只维护一条连接，登录后建立、退出时关闭。
 *     绝不在每个聊天页面里 new WebSocket()，否则来回切页面会建立一堆连接。
 *  2. 心跳保活：每 30 秒发 PING，10 秒内没等到 PONG 就判定连接已死并重连。
 *     没有心跳的话，连接可能已被中间的代理（Nginx、natapp 隧道）悄悄断开，
 *     而浏览器还以为连着，消息发出去石沉大海。
 *  3. 指数退避重连：1s → 2s → 4s → ... 最多 30s，避免服务端重启时被客户端集体冲击。
 *  4. 重连成功回调：让上层去补拉离线消息，这是「断线期间消息不丢」的关键一步。
 */

const HEARTBEAT_INTERVAL = 30000
const HEARTBEAT_TIMEOUT = 10000
const RECONNECT_BASE = 1000
const RECONNECT_MAX = 30000

class ChatSocket {
  constructor() {
    this.ws = null
    this.listeners = new Map()
    this.reconnectTimer = null
    this.heartbeatTimer = null
    this.pongTimer = null
    this.reconnectAttempts = 0
    this.manualClosed = false
    this.connected = false
  }

  /** 建立连接（已连接或正在连接时不做重复操作） */
  connect() {
    const token = localStorage.getItem('token')
    if (!token) {
      return
    }
    if (this.ws && (this.ws.readyState === WebSocket.OPEN || this.ws.readyState === WebSocket.CONNECTING)) {
      return
    }

    this.manualClosed = false
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
    // 走同源路径，由 Vite 代理转发到后端（后端 context-path 为 /api）
    const url = `${protocol}//${window.location.host}/api/ws/chat?token=${encodeURIComponent(token)}`

    try {
      this.ws = new WebSocket(url)
    } catch (e) {
      this.scheduleReconnect()
      return
    }

    this.ws.onopen = () => {
      this.connected = true
      this.reconnectAttempts = 0
      this.startHeartbeat()
      this.emit('open')
    }

    this.ws.onmessage = (event) => {
      let frame
      try {
        frame = JSON.parse(event.data)
      } catch (e) {
        return
      }
      if (frame.type === 'PONG') {
        this.clearPongTimer()
        return
      }
      this.emit(frame.type, frame.data)
    }

    this.ws.onclose = () => {
      this.connected = false
      this.stopHeartbeat()
      this.emit('close')
      if (!this.manualClosed) {
        this.scheduleReconnect()
      }
    }

    this.ws.onerror = () => {
      // onerror 之后必定触发 onclose，重连逻辑统一放在 onclose 里
    }
  }

  /** 主动断开（退出登录时调用），不会再自动重连 */
  close() {
    this.manualClosed = true
    this.stopHeartbeat()
    this.clearReconnectTimer()
    if (this.ws) {
      try {
        this.ws.close()
      } catch (e) {
        /* 忽略 */
      }
      this.ws = null
    }
    this.connected = false
  }

  /** 发送一帧；连接未就绪时返回 false，由调用方决定是否走 REST 兜底 */
  send(type, data) {
    if (!this.ws || this.ws.readyState !== WebSocket.OPEN) {
      return false
    }
    try {
      this.ws.send(JSON.stringify({ type, data, ts: Date.now() }))
      return true
    } catch (e) {
      return false
    }
  }

  /* ---------- 事件订阅 ---------- */

  on(type, handler) {
    if (!this.listeners.has(type)) {
      this.listeners.set(type, new Set())
    }
    this.listeners.get(type).add(handler)
  }

  off(type, handler) {
    const set = this.listeners.get(type)
    if (set) {
      set.delete(handler)
    }
  }

  emit(type, data) {
    const set = this.listeners.get(type)
    if (set) {
      set.forEach((fn) => {
        try {
          fn(data)
        } catch (e) {
          console.error(`[ws] 处理 ${type} 事件出错`, e)
        }
      })
    }
  }

  /* ---------- 心跳 ---------- */

  startHeartbeat() {
    this.stopHeartbeat()
    this.heartbeatTimer = setInterval(() => {
      if (!this.send('PING')) {
        return
      }
      // 发出 PING 后开始计时，超时未收到 PONG 视为连接已死
      this.clearPongTimer()
      this.pongTimer = setTimeout(() => {
        if (this.ws) {
          try {
            this.ws.close()
          } catch (e) {
            /* 忽略 */
          }
        }
      }, HEARTBEAT_TIMEOUT)
    }, HEARTBEAT_INTERVAL)
  }

  stopHeartbeat() {
    if (this.heartbeatTimer) {
      clearInterval(this.heartbeatTimer)
      this.heartbeatTimer = null
    }
    this.clearPongTimer()
  }

  clearPongTimer() {
    if (this.pongTimer) {
      clearTimeout(this.pongTimer)
      this.pongTimer = null
    }
  }

  /* ---------- 重连 ---------- */

  scheduleReconnect() {
    this.clearReconnectTimer()
    const delay = Math.min(RECONNECT_BASE * Math.pow(2, this.reconnectAttempts), RECONNECT_MAX)
    this.reconnectAttempts += 1
    this.reconnectTimer = setTimeout(() => {
      // 重连前不强行刷新 token：token 失效会由 HTTP 层统一处理跳登录
      this.connect()
    }, delay)
  }

  clearReconnectTimer() {
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer)
      this.reconnectTimer = null
    }
  }
}

export const chatSocket = new ChatSocket()

export default chatSocket
