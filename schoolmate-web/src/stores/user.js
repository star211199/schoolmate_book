import { defineStore } from 'pinia'
import { login as loginApi, getMe } from '@/api/auth'

/**
 * 用户状态：Token + 用户信息，持久化到 localStorage。
 */
export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    userInfo: JSON.parse(localStorage.getItem('userInfo') || 'null')
  }),
  getters: {
    isLogin: (state) => !!state.token,
    isAdmin: (state) => state.userInfo?.role === 'ADMIN',
    userId: (state) => state.userInfo?.userId || state.userInfo?.id || null
  },
  actions: {
    /** 登录并拉取用户信息 */
    async login(loginForm) {
      const res = await loginApi(loginForm)
      this.token = res.data.token
      localStorage.setItem('token', res.data.token)
      await this.fetchUserInfo()
      return res
    },

    /** 拉取当前登录用户 */
    async fetchUserInfo() {
      const res = await getMe()
      this.userInfo = res.data
      localStorage.setItem('userInfo', JSON.stringify(res.data))
      return res.data
    },

    /** 退出登录 */
    logout() {
      this.token = ''
      this.userInfo = null
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
    }
  }
})
