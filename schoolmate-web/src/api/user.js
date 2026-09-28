import request from '@/utils/request'

/** 用户信息 */
export function getUser(id) {
  return request.get(`/users/${id}`)
}

/** 更新用户信息 */
export function updateUser(id, data) {
  return request.put(`/users/${id}`, data)
}

/** 修改密码 */
export function updatePassword(id, data) {
  return request.put(`/users/${id}/password`, data)
}

/** 个人资料 */
export function getProfile(id) {
  return request.get(`/users/${id}/profile`)
}

/** 更新个人资料 */
export function updateProfile(id, data) {
  return request.put(`/users/${id}/profile`, data)
}

/** 上传文件 */
export function uploadFile(formData) {
  return request.post('/files/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}
