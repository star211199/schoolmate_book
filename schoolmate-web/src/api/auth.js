import request from '@/utils/request'

/**
 * 认证相关接口。
 */
export function login(data) {
  return request.post('/auth/login', data)
}

export function register(data) {
  return request.post('/auth/register', data)
}

export function getMe() {
  return request.get('/auth/me')
}
