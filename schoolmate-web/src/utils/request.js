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
  // 公网演示走的是 1Mbps 的免费穿透隧道，首屏资源与首个接口请求会互相抢占带宽。
  // 15s 在慢网上偏紧（曾经表现为点登录就报「网络异常」），放宽到 25s。
  timeout: 25000
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
    // 区分「超时」「连不上」「服务端返回了非 2xx 但没有标准 message」三种情况，
    // 避免所有失败都被笼统压成一句「网络异常」，掩盖真实状态码（排查时很致命）。
    let msg = error.response?.data?.message
    if (!msg) {
      if (error.code === 'ECONNABORTED' || /timeout/i.test(error.message || '')) {
        msg = '请求超时，当前网络较慢，请重试'
      } else if (!error.response) {
        msg = '网络连接失败，请检查网络后重试'
      } else {
        // 后端返回了响应，但响应体不是标准 Result（网关错误页、CORS 拒绝等）
        const raw = error.response.data
        const text = typeof raw === 'string' ? raw.replace(/<[^>]*>/g, ' ').replace(/\s+/g, ' ').trim() : ''
        msg = `请求失败（HTTP ${status}）${text ? '：' + text.slice(0, 60) : ''}`
      }
    }
    ElMessage.error(msg || '网络异常，请稍后重试')
    return Promise.reject(error)
  }
)

export default request
