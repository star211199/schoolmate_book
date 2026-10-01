import request from '@/utils/request'

/** 后台：分页查询用户 */
export function pageUsers(params) {
  return request.get('/admin/users', { params })
}

/** 后台：启停用户 */
export function updateUserStatus(id, status) {
  return request.put(`/admin/users/${id}/status`, null, { params: { status } })
}

/** 后台：重置用户密码（返回随机新密码，仅显示一次） */
export function resetUserPassword(id) {
  return request.put(`/admin/users/${id}/reset-password`)
}

/** 后台：仪表盘统计 */
export function getStats() {
  return request.get('/admin/stats')
}

/** 后台：待审核列表 */
export function listPendingAudits(type) {
  return request.get('/admin/audit/pending', { params: { type } })
}

/** 后台：审核内容 */
export function auditContent(type, id, passed) {
  return request.put(`/admin/audit/${type}/${id}`, null, { params: { passed } })
}
