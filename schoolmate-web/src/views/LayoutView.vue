<template>
  <el-container class="layout">
    <el-header class="header">
      <div class="logo" @click="$router.push('/classes')">
        <el-icon :size="22"><School /></el-icon>
        <span>大学同学录</span>
      </div>

      <el-menu
        :default-active="activeMenu"
        mode="horizontal"
        :router="true"
        :ellipsis="false"
        class="nav-menu"
      >
        <el-menu-item index="/classes">班级广场</el-menu-item>
        <el-menu-item index="/profile">个人中心</el-menu-item>
        <el-menu-item v-if="isAdmin" index="/admin">后台管理</el-menu-item>
      </el-menu>

      <div class="right">
        <el-dropdown v-if="isLogin" @command="handleCommand">
          <span class="user-info">
            <el-avatar :size="32" :src="avatar">
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
  display: flex;
  align-items: center;
  gap: 24px;
  background: #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  padding: 0 24px;
}

.logo {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 18px;
  font-weight: 600;
  color: var(--el-color-primary);
  cursor: pointer;
  white-space: nowrap;
}

.nav-menu {
  flex: 1;
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
  padding: 0;
  background: var(--bg-color);
}
</style>
