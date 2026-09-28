import request from '@/utils/request'

/** 班级相册列表 */
export function listAlbums(classId) {
  return request.get(`/classes/${classId}/albums`)
}

/** 创建相册 */
export function createAlbum(classId, data) {
  return request.post(`/classes/${classId}/albums`, data)
}

/** 相册详情 */
export function getAlbum(id) {
  return request.get(`/albums/${id}`)
}

/** 删除相册 */
export function deleteAlbum(id) {
  return request.delete(`/albums/${id}`)
}

/** 相册照片列表 */
export function listPhotos(albumId) {
  return request.get(`/albums/${albumId}/photos`)
}

/** 上传照片 */
export function uploadPhoto(albumId, formData) {
  return request.post(`/albums/${albumId}/photos`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

/** 删除照片 */
export function deletePhoto(id) {
  return request.delete(`/photos/${id}`)
}
