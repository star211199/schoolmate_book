import request from '@/utils/request'

/** 分页查询班级动态（时间轴） */
export function pageMoments(classId, params) {
  return request.get(`/classes/${classId}/moments`, { params })
}

/** 发布动态 */
export function createMoment(classId, data) {
  return request.post(`/classes/${classId}/moments`, data)
}

/** 修改动态 */
export function updateMoment(id, data) {
  return request.put(`/moments/${id}`, data)
}

/** 删除动态 */
export function deleteMoment(id) {
  return request.delete(`/moments/${id}`)
}

/** 评论列表 */
export function listComments(momentId) {
  return request.get(`/moments/${momentId}/comments`)
}

/** 发表评论 */
export function createComment(momentId, data) {
  return request.post(`/moments/${momentId}/comments`, data)
}

/** 删除评论 */
export function deleteComment(id) {
  return request.delete(`/comments/${id}`)
}

/** 点赞 / 取消点赞（切换） */
export function toggleLike(momentId) {
  return request.post(`/moments/${momentId}/like`)
}
