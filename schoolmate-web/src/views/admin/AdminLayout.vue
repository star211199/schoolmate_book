<template>
  <div class="page-container">
    <el-card class="card-shadow">
      <el-menu :default-active="activeMenu" mode="horizontal" :router="true" :ellipsis="false">
        <el-menu-item index="/admin/dashboard">数据概览</el-menu-item>
        <el-menu-item index="/admin/users">用户管理</el-menu-item>
        <el-menu-item index="/admin/audit">内容审核</el-menu-item>
      </el-menu>
      <router-view />
    </el-card>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const activeMenu = computed(() => route.path)
</script>

<style scoped>
:deep(.el-menu) {
  border-bottom: none;
  margin-bottom: 12px;
}
</style>
