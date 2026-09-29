import request from '@/utils/request'

/**
 * 会话与消息接口。
 *
 * 划分原则：拉取数据走 REST，实时收发走 WebSocket。
 * 这样 WebSocket 断开时页面依然能正常浏览历史消息。
 */

/** 我的会话列表 */
export function getSessions() {
  return request.get('/chat/sessions')
}

/** 会话详情 */
export function getSessionDetail(sessionId) {
  return request.get(`/chat/sessions/${sessionId}`)
}

/** 获取或创建与某用户的私聊会话，返回会话ID */
export function openPrivateSession(targetUserId) {
  return request.post(`/chat/sessions/private/${targetUserId}`)
}

/** 历史消息（向上翻页，beforeId 为空取最新一页） */
export function getMessages(sessionId, beforeId, size = 30) {
  return request.get(`/chat/sessions/${sessionId}/messages`, {
    params: { beforeId, size }
  })
}

/** 增量拉取（断线重连后补齐） */
export function getMessagesAfter(sessionId, afterId) {
  return request.get(`/chat/sessions/${sessionId}/messages/after`, {
    params: { afterId }
  })
}

/** 发送消息（WebSocket 不可用时的兜底通道） */
export function sendMessageRest(data) {
  return request.post('/chat/messages', data)
}

/** 上报已读 */
export function markReadRest(sessionId, lastReadMessageId) {
  return request.put(`/chat/sessions/${sessionId}/read`, null, {
    params: { lastReadMessageId }
  })
}

/** 撤回消息 */
export function recallMessage(messageId) {
  return request.put(`/chat/messages/${messageId}/recall`)
}
