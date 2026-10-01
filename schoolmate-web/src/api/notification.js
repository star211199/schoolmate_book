import request from '@/utils/request'

/**
 * 通知中心接口。
 */

/** 通知分页列表（unreadOnly=true 时只看未读） */
export function getNotifications(params) {
  return request.get('/notifications', { params })
}

/** 未读通知数（铃铛角标） */
export function getUnreadCount() {
  return request.get('/notifications/unread-count')
}

/** 标记单条已读 */
export function markNotificationRead(id) {
  return request.put(`/notifications/${id}/read`)
}

/** 全部标记已读 */
export function markAllNotificationsRead() {
  return request.put('/notifications/read-all')
}
