import axios from 'axios'
import { ElMessage } from 'element-plus'

/**
 * Axios 封装：
 * - 统一 baseURL（开发期走 Vite 代理 /api → http://localhost:8080）
 * - 请求拦截自动携带 Token
 * - 响应拦截统一解析 Result，按 code 判定成败，401 跳登录
 */
const request = axios.create({
  baseURL: '/api',
  timeout: 15000
})

// 请求拦截器
request.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// 响应拦截器
request.interceptors.response.use(
  (response) => {
    const res = response.data
    // 二进制数据（如文件下载）直接返回
    if (response.config.responseType === 'blob') {
      return response
    }
    if (res.code === 20000) {
      return res
    }
    ElMessage.error(res.message || '请求失败')
    return Promise.reject(new Error(res.message || '请求失败'))
  },
  (error) => {
    const status = error.response?.status
    if (status === 401) {
      ElMessage.error('登录已过期，请重新登录')
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
      setTimeout(() => {
        window.location.href = '/login'
      }, 800)
      return Promise.reject(error)
    }
    ElMessage.error(error.response?.data?.message || '网络异常，请稍后重试')
    return Promise.reject(error)
  }
)

export default request
