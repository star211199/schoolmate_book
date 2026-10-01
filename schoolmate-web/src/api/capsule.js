import request from '@/utils/request'

/**
 * 时光胶囊接口。
 */

/** 写一封信并封存 */
export function createCapsule(data) {
  return request.post('/capsules', data)
}

/** 我的胶囊列表（含倒计时，未到点不返回内容） */
export function getMyCapsules() {
  return request.get('/capsules/my')
}

/** 班级公开胶囊墙 */
export function getClassCapsules(classId) {
  return request.get(`/capsules/class/${classId}`)
}

/** 胶囊详情 */
export function getCapsule(id) {
  return request.get(`/capsules/${id}`)
}

/** 删除胶囊（仅本人且封存中） */
export function deleteCapsule(id) {
  return request.delete(`/capsules/${id}`)
}
