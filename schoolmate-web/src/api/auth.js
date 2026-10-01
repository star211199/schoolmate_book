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

/** 是否已配置邮件服务（未配置则隐藏邮箱找回入口） */
export function getMailResetEnabled() {
  return request.get('/auth/mail-reset-enabled')
}

/** 找回密码第一步：校验账号+邮箱并发送 6 位验证码 */
export function forgotPassword(data) {
  return request.post('/auth/forgot-password', data)
}

/** 找回密码第二步：凭验证码重置密码 */
export function resetPassword(data) {
  return request.post('/auth/reset-password', data)
}
