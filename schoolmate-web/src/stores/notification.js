import { defineStore } from 'pinia'
import { ref } from 'vue'
import { chatSocket } from '@/utils/ws'
import { getUnreadCount } from '@/api/notification'

/**
 * 通知中心仓库。
 *
 * 职责很窄：只维护「未读通知数」这一个状态（导航栏铃铛角标），
 * 并监听 WebSocket 的 NOTIFICATION 帧做实时 +1。
 * 通知列表数据不做缓存，由通知页自己按需拉取 —— 列表是低频页面，
 * 缓存反而引入「列表与角标不一致」的同步问题。
 */
export const useNotificationStore = defineStore('notification', () => {
  /** 未读通知数 */
  const unreadCount = ref(0)

  async function refreshUnread() {
    try {
      const res = await getUnreadCount()
      unreadCount.value = res.data || 0
    } catch {
      // 角标不是关键路径，失败时静默（比如刚登出、token 过期）
    }
  }

  /**
   * 进入主框架后调用：拉一次未读数，并订阅实时通知帧。
   * chatSocket 是多监听者模型，这里注册不会干扰 chat store 的帧处理。
   */
  function bindRealtime() {
    refreshUnread()
    chatSocket.on('NOTIFICATION', () => {
      unreadCount.value += 1
    })
  }

  /** 标记已读后在本地同步角标（避免每次都重新拉） */
  function decrease(n = 1) {
    unreadCount.value = Math.max(0, unreadCount.value - n)
  }

  function reset() {
    unreadCount.value = 0
  }

  return { unreadCount, refreshUnread, bindRealtime, decrease, reset }
})
