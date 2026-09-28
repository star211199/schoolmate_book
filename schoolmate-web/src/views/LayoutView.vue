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
        <el-menu-item index="/h5">H5 同学录</el-menu-item>
        <el-menu-item index="/profile">个人中心</el-menu-item>
        <el-menu-item v-if="isAdmin" index="/admin">后台管理</el-menu-item>
      </el-menu>

      <div class="right">
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
import { computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessageBox } from 'element-plus'
import SakuraFall from '@/components/SakuraFall.vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const isLogin = computed(() => userStore.isLogin)
const isAdmin = computed(() => userStore.isAdmin)
const userInfo = computed(() => userStore.userInfo)
const avatar = computed(() => userInfo.value?.avatar || '')
const activeMenu = computed(() => {
  if (route.path.startsWith('/admin')) return '/admin'
  if (route.path.startsWith('/profile')) return '/profile'
  if (route.path.startsWith('/h5')) return '/h5'
  return '/classes'
})

function handleCommand(command) {
  if (command === 'profile') {
    router.push('/profile')
  } else if (command === 'logout') {
    ElMessageBox.confirm('确定要退出登录吗？', '提示', { type: 'warning' })
      .then(() => {
        userStore.logout()
        router.push('/login')
      })
      .catch(() => {})
  }
}
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
  background: rgba(255, 255, 255, 0.72);
  backdrop-filter: blur(14px);
  -webkit-backdrop-filter: blur(14px);
  box-shadow: 0 2px 16px rgba(251, 111, 146, 0.1);
  border-bottom: 1px solid rgba(255, 255, 255, 0.9);
  padding: 0 24px;
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
