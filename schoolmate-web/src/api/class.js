import request from '@/utils/request'

/** 分页查询班级 */
export function pageClasses(params) {
  return request.get('/classes', { params })
}

/** 我加入的班级 */
export function myClasses() {
  return request.get('/classes/my')
}

/** 班级详情 */
export function getClassDetail(id) {
  return request.get(`/classes/${id}`)
}

/** 创建班级 */
export function createClass(data) {
  return request.post('/classes', data)
}

/** 更新班级 */
export function updateClass(id, data) {
  return request.put(`/classes/${id}`, data)
}

/** 解散班级 */
export function deleteClass(id) {
  return request.delete(`/classes/${id}`)
}

/** 加入班级 */
export function joinClass(data) {
  return request.post('/classes/join', data)
}

/** 班级成员列表 */
export function listMembers(classId) {
  return request.get(`/classes/${classId}/members`)
}

/** 移出成员 */
export function removeMember(classId, userId) {
  return request.delete(`/classes/${classId}/members/${userId}`)
}

/** 生日提醒：未来 N 天内过生日的成员 */
export function birthdayReminders(classId, withinDays = 30) {
  return request.get(`/classes/${classId}/birthday-reminders`, { params: { withinDays } })
}
