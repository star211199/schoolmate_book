import request from '@/utils/request'

/**
 * 好友接口。
 */

/** 我的好友列表 */
export function getFriends() {
  return request.get('/friends')
}

/** 删除好友 */
export function deleteFriend(friendUserId) {
  return request.delete(`/friends/${friendUserId}`)
}

/** 设置备注名与分组 */
export function updateFriend(friendUserId, data) {
  return request.put(`/friends/${friendUserId}`, data)
}

/** 收到的好友申请 */
export function getReceivedRequests(status) {
  return request.get('/friends/requests', { params: { status } })
}

/** 我发出的好友申请 */
export function getSentRequests() {
  return request.get('/friends/requests/sent')
}

/** 待处理申请数量（红点） */
export function getPendingCount() {
  return request.get('/friends/requests/pending-count')
}

/** 发起好友申请 */
export function sendFriendRequest(data) {
  return request.post('/friends/requests', data)
}

/** 同意申请 */
export function acceptRequest(requestId) {
  return request.put(`/friends/requests/${requestId}/accept`)
}

/** 拒绝申请 */
export function rejectRequest(requestId) {
  return request.put(`/friends/requests/${requestId}/reject`)
}

/** 搜索用户 */
export function searchUsers(keyword) {
  return request.get('/friends/search', { params: { keyword } })
}

/** 查询我与某用户的关系 */
export function getRelation(targetUserId) {
  return request.get(`/friends/relation/${targetUserId}`)
}
