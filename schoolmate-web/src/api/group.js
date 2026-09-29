import request from '@/utils/request'

/**
 * 群聊接口。
 */

/** 我加入的群 */
export function getMyGroups() {
  return request.get('/groups')
}

/** 创建群聊 */
export function createGroup(data) {
  return request.post('/groups', data)
}

/** 群资料 */
export function getGroupDetail(groupId) {
  return request.get(`/groups/${groupId}`)
}

/** 修改群资料 */
export function updateGroup(groupId, data) {
  return request.put(`/groups/${groupId}`, data)
}

/** 解散群 */
export function dismissGroup(groupId) {
  return request.delete(`/groups/${groupId}`)
}

/** 群成员分页列表 */
export function getGroupMembers(groupId, pageNum = 1, pageSize = 20) {
  return request.get(`/groups/${groupId}/members`, { params: { pageNum, pageSize } })
}

/** 批量拉人入群 */
export function addGroupMembers(groupId, userIds) {
  return request.post(`/groups/${groupId}/members`, { userIds })
}

/** 移出成员（传自己的ID等同于退群） */
export function removeGroupMember(groupId, userId) {
  return request.delete(`/groups/${groupId}/members/${userId}`)
}

/** 退出群聊 */
export function quitGroup(groupId) {
  return request.post(`/groups/${groupId}/quit`)
}
