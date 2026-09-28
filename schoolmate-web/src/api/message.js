import request from '@/utils/request'

/** 分页查询班级留言 */
export function pageMessages(classId, params) {
  return request.get(`/classes/${classId}/messages`, { params })
}

/** 发布留言 */
export function createMessage(classId, data) {
  return request.post(`/classes/${classId}/messages`, data)
}

/** 删除留言 */
export function deleteMessage(id) {
  return request.delete(`/messages/${id}`)
}
