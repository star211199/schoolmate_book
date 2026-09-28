<template>
  <div v-loading="loading">
    <el-row :gutter="16">
      <el-col v-for="card in cards" :key="card.label" :xs="12" :sm="8" :md="6">
        <el-card class="stat-card card-shadow">
          <div class="stat-value">{{ card.value }}</div>
          <div class="stat-label text-muted">{{ card.label }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-alert
      v-if="stats?.pendingCount > 0"
      type="warning"
      show-icon
      :closable="false"
      class="tip"
    >
      当前有 {{ stats.pendingCount }} 条内容待审核，请前往「内容审核」处理。
    </el-alert>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getStats } from '@/api/admin'

const loading = ref(false)
const stats = ref(null)

const cards = computed(() => [
  { label: '注册用户', value: stats.value?.userCount ?? 0 },
  { label: '班级数量', value: stats.value?.classCount ?? 0 },
  { label: '班级留言', value: stats.value?.messageCount ?? 0 },
  { label: '班级动态', value: stats.value?.momentCount ?? 0 },
  { label: '相册数量', value: stats.value?.albumCount ?? 0 },
  { label: '照片数量', value: stats.value?.photoCount ?? 0 }
])

onMounted(async () => {
  loading.value = true
  try {
    const res = await getStats()
    stats.value = res.data
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.stat-card {
  text-align: center;
  margin-bottom: 16px;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: var(--el-color-primary);
}

.tip {
  margin-top: 8px;
}
</style>
