import { defineStore } from 'pinia'
import { ref, computed, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import chatSocket from '@/utils/ws'
import {
  getSessions,
  getMessages,
  getMessagesAfter,
  sendMessageRest,
  markReadRest,
  openPrivateSession
} from '@/api/chat'
import { getPendingCount } from '@/api/friend'
import { useUserStore } from '@/stores/user'

/** 生成形如 2026-09-29 10:30:00 的本地时间串 */
function nowText() {
  const d = new Date()
  const p = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`
}

/** 会话列表的最后一条消息摘要（与后端 buildPreview 保持一致的观感） */
function previewOf(msg, session) {
  if (!msg) return ''
  let prefix = ''
  if (session && session.sessionType === 'GROUP' && msg.msgType !== 'SYSTEM') {
    prefix = msg.senderId === useUserStore().userId ? '我：' : `${msg.senderDisplayName || msg.senderNickname || ''}：`
  }
  const body =
    {
      SYSTEM: msg.content || '',
      RECALL: '消息已撤回',
      IMAGE: '[图片]',
      FILE: '[文件]'
    }[msg.msgType] || msg.content || ''
  return prefix + body
}

/**
 * 聊天状态仓库。
 *
 * 三个关键职责：
 *  1. 把 WebSocket 事件翻译成界面状态（会话列表、未读数、消息流）
 *  2. 乐观更新：发消息先本地插入气泡，收到回执再确认，用户无需等待网络
 *  3. 断线重连后补拉：会话列表全量刷新 + 当前会话增量补齐，保证不丢消息
 */
export const useChatStore = defineStore('chat', () => {
  const userStore = useUserStore()

  const sessions = ref([])
  /** { [sessionId]: ChatMessage[] } */
  const messages = ref({})
  const activeSessionId = ref(null)
  const connected = ref(false)
  const loadingSessions = ref(false)
  const friendRequestCount = ref(0)
  const bound = ref(false)

  /** 导航栏未读总数 */
  const totalUnread = computed(() =>
    sessions.value.reduce((sum, s) => sum + (s.unreadCount || 0), 0)
  )

  const activeSession = computed(
    () => sessions.value.find((s) => s.id === activeSessionId.value) || null
  )

  function activeMessages() {
    return messages.value[activeSessionId.value] || []
  }

  /* ==================== 连接生命周期 ==================== */

  /** 登录后调用：绑定事件并建立连接 */
  function connect() {
    if (!userStore.isLogin) return
    bindEvents()
    chatSocket.connect()
    loadFriendRequestCount()
  }

  /** 退出登录时调用 */
  function disconnect() {
    chatSocket.close()
    connected.value = false
    sessions.value = []
    messages.value = {}
    activeSessionId.value = null
    friendRequestCount.value = 0
  }

  function bindEvents() {
    if (bound.value) return
    bound.value = true

    chatSocket.on('open', async () => {
      connected.value = true
      // 重连后补拉：会话列表（拿最新未读数） + 当前会话的增量消息
      await loadSessions()
      await syncActiveSession()
    })

    chatSocket.on('close', () => {
      connected.value = false
    })

    chatSocket.on('CHAT_MESSAGE', handleIncomingMessage)
    chatSocket.on('MESSAGE_ACK', (msg) => upsertMessage(msg.sessionId, msg))
    chatSocket.on('READ_NOTIFY', () => {
      /* 已读回执：P0 暂不展示，P2 再补「对方已读」标记 */
    })

    chatSocket.on('FRIEND_REQUEST', () => {
      friendRequestCount.value += 1
      ElMessage.info('收到一条新的好友申请')
    })

    chatSocket.on('FRIEND_ACCEPTED', async () => {
      await loadSessions()
      ElMessage.success('对方已同意你的好友申请')
    })

    chatSocket.on('GROUP_INVITE', async () => {
      await loadSessions()
      ElMessage.success('你被邀请加入了新的群聊')
    })

    chatSocket.on('GROUP_CHANGED', async () => {
      await loadSessions()
    })

    chatSocket.on('ERROR', (data) => {
      if (data && data.message) {
        ElMessage.error(data.message)
      }
    })
  }

  /* ==================== 数据加载 ==================== */

  async function loadSessions() {
    if (!userStore.isLogin) return
    loadingSessions.value = true
    try {
      const res = await getSessions()
      sessions.value = res.data || []
    } finally {
      loadingSessions.value = false
    }
  }

  async function loadFriendRequestCount() {
    try {
      const res = await getPendingCount()
      friendRequestCount.value = res.data || 0
    } catch (e) {
      /* 忽略 */
    }
  }

  /** 打开会话：加载历史 + 标记已读 */
  async function openSession(sessionId) {
    activeSessionId.value = sessionId
    if (!messages.value[sessionId]) {
      await loadHistory(sessionId)
    }
    await markCurrentRead(sessionId)
  }

  async function loadHistory(sessionId, beforeId = null) {
    const res = await getMessages(sessionId, beforeId, 30)
    const list = res.data || []
    if (beforeId) {
      messages.value[sessionId] = [...list, ...(messages.value[sessionId] || [])]
    } else {
      messages.value[sessionId] = list
    }
    return list
  }

  /** 向上翻页加载更多历史 */
  async function loadMore(sessionId) {
    const list = messages.value[sessionId] || []
    const firstReal = list.find((m) => typeof m.id === 'number')
    if (!firstReal) return []
    return loadHistory(sessionId, firstReal.id)
  }

  /** 与某个用户开启私聊（没有会话则后端创建） */
  async function startPrivateChat(targetUserId) {
    const res = await openPrivateSession(targetUserId)
    const sessionId = res.data
    await loadSessions()
    await openSession(sessionId)
    return sessionId
  }

  /* ==================== 消息收发 ==================== */

  /**
   * 发送消息。
   *
   * 乐观更新：先在本地插入一条「发送中」气泡，用户点发送立刻能看到，
   * 不等网络往返。随后收到的 CHAT_MESSAGE / MESSAGE_ACK 会把这条临时消息
   * 按 clientMsgId 替换成真实消息。
   */
  function sendMessage(sessionId, content, msgType = 'TEXT', extra = null) {
    const clientMsgId = `${userStore.userId}_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`
    const temp = {
      id: `temp_${clientMsgId}`,
      sessionId,
      senderId: userStore.userId,
      senderNickname: userStore.userInfo?.nickname,
      senderAvatar: userStore.userInfo?.avatar,
      senderDisplayName: userStore.userInfo?.nickname,
      msgType,
      content,
      extra,
      clientMsgId,
      sendTime: nowText(),
      _pending: true
    }
    pushMessage(sessionId, temp)
    updateSessionPreview(sessionId, temp)

    const payload = { sessionId, msgType, content, clientMsgId }
    if (extra) {
      payload.extra = extra
    }

    const sent = chatSocket.send('CHAT_SEND', payload)
    if (!sent) {
      // WebSocket 不可用 → 走 REST 兜底，保证消息仍能发出去
      sendMessageRest(payload)
        .then((res) => upsertMessage(sessionId, res.data))
        .catch(() => markFailed(sessionId, clientMsgId))
    }
  }

  /** 重发失败的消息 */
  function resendMessage(message) {
    const sessionId = message.sessionId
    message._failed = false
    message._pending = true
    const payload = {
      sessionId,
      msgType: message.msgType,
      content: message.content,
      clientMsgId: message.clientMsgId
    }
    const sent = chatSocket.send('CHAT_SEND', payload)
    if (!sent) {
      sendMessageRest(payload)
        .then((res) => upsertMessage(sessionId, res.data))
        .catch(() => markFailed(sessionId, message.clientMsgId))
    }
  }

  /** 上报已读 */
  function reportRead(sessionId, lastReadMessageId) {
    const sent = chatSocket.send('READ_ACK', { sessionId, lastReadMessageId })
    if (!sent) {
      markReadRest(sessionId, lastReadMessageId).catch(() => {
        /* 忽略：下次打开会话会重新上报 */
      })
    }
  }

  async function markCurrentRead(sessionId) {
    const list = messages.value[sessionId] || []
    const lastReal = [...list].reverse().find((m) => typeof m.id === 'number')
    if (lastReal) {
      reportRead(sessionId, lastReal.id)
    }
    const session = sessions.value.find((s) => s.id === sessionId)
    if (session) {
      session.unreadCount = 0
    }
  }

  /* ==================== WebSocket 事件处理 ==================== */

  function handleIncomingMessage(msg) {
    const sessionId = msg.sessionId
    upsertMessage(sessionId, msg)

    const index = sessions.value.findIndex((s) => s.id === sessionId)
    if (index < 0) {
      // 收到一个列表里还没有的会话（例如刚被拉进群），重新拉一次
      loadSessions()
      return
    }

    const session = sessions.value[index]
    session.lastMessageId = msg.id
    session.lastMessageTime = msg.sendTime
    session.lastMessagePreview = previewOf(msg, session)

    const isMine = msg.senderId === userStore.userId
    if (sessionId === activeSessionId.value) {
      // 正在看这个会话，读到即已读
      session.unreadCount = 0
      if (!isMine && typeof msg.id === 'number') {
        reportRead(sessionId, msg.id)
      }
    } else if (!isMine) {
      session.unreadCount = (session.unreadCount || 0) + 1
    }

    sortSessions()
  }

  /** 重连后补齐当前会话在断线期间错过的消息 */
  async function syncActiveSession() {
    const sessionId = activeSessionId.value
    if (!sessionId) return
    const list = messages.value[sessionId] || []
    const lastReal = [...list].reverse().find((m) => typeof m.id === 'number')
    if (!lastReal) {
      await loadHistory(sessionId)
      return
    }
    try {
      const res = await getMessagesAfter(sessionId, lastReal.id)
      ;(res.data || []).forEach((m) => upsertMessage(sessionId, m))
    } catch (e) {
      /* 忽略：下次打开会话会重新拉取 */
    }
  }

  /* ==================== 内部工具 ==================== */

  /** 插入或替换消息：优先按真实 id 匹配，其次按 clientMsgId 匹配临时消息 */
  function upsertMessage(sessionId, msg) {
    const list = messages.value[sessionId]
    if (!list) return
    let index = list.findIndex((m) => m.id === msg.id)
    if (index < 0 && msg.clientMsgId) {
      index = list.findIndex((m) => m.clientMsgId === msg.clientMsgId)
    }
    if (index >= 0) {
      list[index] = { ...list[index], ...msg, _pending: false, _failed: false }
    } else {
      list.push(msg)
    }
  }

  function pushMessage(sessionId, msg) {
    if (!messages.value[sessionId]) {
      messages.value[sessionId] = []
    }
    messages.value[sessionId].push(msg)
  }

  function markFailed(sessionId, clientMsgId) {
    const list = messages.value[sessionId] || []
    const target = list.find((m) => m.clientMsgId === clientMsgId)
    if (target) {
      target._pending = false
      target._failed = true
    }
  }

  function updateSessionPreview(sessionId, msg) {
    const session = sessions.value.find((s) => s.id === sessionId)
    if (session) {
      session.lastMessagePreview = previewOf(msg, session)
    }
  }

  /** 置顶优先，其次按最后消息时间倒序 */
  function sortSessions() {
    sessions.value.sort((a, b) => {
      if (!!a.pinned !== !!b.pinned) {
        return a.pinned ? -1 : 1
      }
      return String(b.lastMessageTime || '').localeCompare(String(a.lastMessageTime || ''))
    })
  }

  return {
    sessions,
    messages,
    activeSessionId,
    activeSession,
    activeMessages,
    connected,
    loadingSessions,
    friendRequestCount,
    totalUnread,
    connect,
    disconnect,
    bindEvents,
    loadSessions,
    loadFriendRequestCount,
    openSession,
    loadHistory,
    loadMore,
    startPrivateChat,
    sendMessage,
    resendMessage,
    reportRead,
    markCurrentRead
  }
})
