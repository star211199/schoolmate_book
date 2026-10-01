<template>
  <el-container class="layout">
    <SakuraFall :density="16" />
    <el-header class="header">
      <div class="logo" @click="$router.push('/classes')">
        <span class="logo-icon">🌸</span>
        <span class="anime-title">大学同学录</span>
      </div>

      <el-menu
        :default-active="activeMenu"
        mode="horizontal"
        :router="true"
        :ellipsis="false"
        class="nav-menu"
      >
        <el-menu-item index="/classes">班级广场</el-menu-item>
        <el-menu-item index="/chat">
          消息
          <span v-if="chatBadge" class="nav-badge">{{ chatBadge > 99 ? '99+' : chatBadge }}</span>
        </el-menu-item>
        <el-menu-item index="/contacts">
          联系人
          <span v-if="friendBadge" class="nav-badge">{{ friendBadge }}</span>
        </el-menu-item>
        <el-menu-item index="/h5">H5 同学录</el-menu-item>
        <el-menu-item index="/theme">主题装扮</el-menu-item>
        <el-menu-item index="/profile">个人中心</el-menu-item>
        <el-menu-item v-if="isAdmin" index="/admin">后台管理</el-menu-item>
      </el-menu>

      <div class="right">
        <el-badge
          v-if="isLogin"
          :value="notifBadge"
          :hidden="!notifBadge"
          :max="99"
          class="bell-badge"
        >
          <el-icon class="bell-icon" title="通知中心" @click="$router.push('/notifications')">
            <Bell />
          </el-icon>
        </el-badge>
        <el-dropdown v-if="isLogin" @command="handleCommand">
          <span class="user-info">
            <el-avatar :size="32" :src="avatar" class="avatar-ring">
              {{ (userInfo?.nickname || 'U').charAt(0) }}
            </el-avatar>
            <span class="nickname">{{ userInfo?.nickname }}</span>
            <el-icon><ArrowDown /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="profile">个人中心</el-dropdown-item>
              <el-dropdown-item command="capsules">时光胶囊</el-dropdown-item>
              <el-dropdown-item command="notifications">通知中心</el-dropdown-item>
              <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
        <el-button v-else type="primary" @click="$router.push('/login')">登录</el-button>
      </div>
    </el-header>

    <el-main class="main">
      <router-view />
    </el-main>
  </el-container>
</template>

<script setup>
import { computed, onMounted, onBeforeUnmount } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useChatStore } from '@/stores/chat'
import { useThemeStore } from '@/stores/theme'
import { useNotificationStore } from '@/stores/notification'
import { ElMessageBox } from 'element-plus'
import SakuraFall from '@/components/SakuraFall.vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const chatStore = useChatStore()
const themeStore = useThemeStore()
const notificationStore = useNotificationStore()

const isLogin = computed(() => userStore.isLogin)
const isAdmin = computed(() => userStore.isAdmin)
const userInfo = computed(() => userStore.userInfo)
const avatar = computed(() => userInfo.value?.avatar || '')

/** 导航栏红点：消息未读总数、待处理好友申请数、通知中心未读数 */
const chatBadge = computed(() => chatStore.totalUnread)
const friendBadge = computed(() => chatStore.friendRequestCount)
const notifBadge = computed(() => notificationStore.unreadCount)

const activeMenu = computed(() => {
  if (route.path.startsWith('/admin')) return '/admin'
  if (route.path.startsWith('/profile')) return '/profile'
  if (route.path.startsWith('/h5')) return '/h5'
  if (route.path.startsWith('/chat')) return '/chat'
  if (route.path.startsWith('/contacts')) return '/contacts'
  if (route.path.startsWith('/theme')) return '/theme'
  if (route.path.startsWith('/capsules')) return '/capsules'
  return '/classes'
})

function handleCommand(command) {
  if (command === 'profile') {
    router.push('/profile')
  } else if (command === 'capsules') {
    router.push('/capsules')
  } else if (command === 'notifications') {
    router.push('/notifications')
  } else if (command === 'logout') {
    ElMessageBox.confirm('确定要退出登录吗？', '提示', { type: 'warning' })
      .then(() => {
        // 先断开长连接，避免退出后仍在接收消息
        chatStore.disconnect()
        notificationStore.reset()
        userStore.logout()
        router.push('/login')
      })
      .catch(() => {})
  }
}

onMounted(() => {
  themeStore.init()
  // 进入主框架就建立 WebSocket 长连接，这样在任何页面都能实时收到新消息与通知
  chatStore.connect()
  notificationStore.bindRealtime()
})

onBeforeUnmount(() => {
  chatStore.disconnect()
})
</script>

<style scoped>
.layout {
  min-height: 100vh;
}

.header {
  position: relative;
  z-index: 10;
  display: flex;
  align-items: center;
  gap: 24px;
  background: var(--color-glass);
  backdrop-filter: blur(14px);
  -webkit-backdrop-filter: blur(14px);
  box-shadow: 0 2px 16px var(--color-shadow);
  border-bottom: 1px solid var(--color-glass-border);
  padding: 0 24px;
}

/* 导航项上的红点 */
.nav-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 16px;
  height: 16px;
  padding: 0 5px;
  margin-left: 5px;
  border-radius: 999px;
  background: var(--color-danger);
  color: #fff;
  font-size: 10px;
  line-height: 1;
  font-weight: 600;
}

.logo {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 18px;
  font-weight: 700;
  cursor: pointer;
  white-space: nowrap;
}

.logo-icon {
  font-size: 22px;
}

.nav-menu {
  flex: 1;
  border-bottom: none;
  background: transparent;
}

.nav-menu :deep(.el-menu-item) {
  background: transparent;
  border-radius: 999px;
  margin: 12px 4px;
  height: 36px;
  line-height: 36px;
}

.nav-menu :deep(.el-menu-item.is-active) {
  background: var(--el-color-primary-light-9);
  border-bottom: none;
}

.nav-menu :deep(.el-menu--horizontal > .el-menu-item) {
  border-bottom: none;
}

.right {
  display: flex;
  align-items: center;
  gap: 16px;
}

/* 通知铃铛 */
.bell-badge {
  display: flex;
  align-items: center;
}

.bell-icon {
  font-size: 20px;
  cursor: pointer;
  color: var(--color-text);
  transition: transform 0.2s ease, color 0.2s ease;
}

.bell-icon:hover {
  color: var(--el-color-primary);
  transform: rotate(12deg);
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  outline: none;
}

.nickname {
  font-size: 14px;
}

.main {
  position: relative;
  z-index: 2;
  padding: 0;
}
</style>
