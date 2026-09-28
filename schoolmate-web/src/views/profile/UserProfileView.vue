<template>
  <div class="page-container" v-loading="loading">
    <el-button link @click="$router.back()" class="back-btn">
      <el-icon><ArrowLeft /></el-icon> 返回
    </el-button>

    <el-card v-if="user" class="card-shadow">
      <div class="profile-header">
        <el-avatar :size="88" :src="user.avatar">
          {{ (user.nickname || 'U').charAt(0) }}
        </el-avatar>
        <div class="info">
          <h2>{{ profile?.realName || user.nickname }}</h2>
          <div class="text-muted">@{{ user.username }}</div>
          <div class="tags">
            <el-tag v-if="profile?.studentNo" size="small" type="info">
              学号 {{ profile.studentNo }}
            </el-tag>
            <el-tag v-if="profile?.enrollmentYear" size="small">
              {{ profile.enrollmentYear }} 级
            </el-tag>
            <el-tag v-if="profile?.currentCity" size="small" type="success">
              现居 {{ profile.currentCity }}
            </el-tag>
          </div>
        </div>
      </div>

      <el-divider />

      <el-descriptions :column="2" border>
        <el-descriptions-item label="昵称">{{ user.nickname }}</el-descriptions-item>
        <el-descriptions-item label="性别">
          {{ genderText(profile?.gender) }}
        </el-descriptions-item>
        <el-descriptions-item label="生日">{{ profile?.birthday || '—' }}</el-descriptions-item>
        <el-descriptions-item label="籍贯">{{ profile?.hometown || '—' }}</el-descriptions-item>
        <el-descriptions-item label="联系方式">{{ profile?.contact || '—' }}</el-descriptions-item>
        <el-descriptions-item label="注册时间">
          {{ (user.createTime || '').replace('T', ' ').slice(0, 16) || '—' }}
        </el-descriptions-item>
        <el-descriptions-item label="个性签名" :span="2">
          {{ profile?.motto || '这位同学很神秘，还没有留下签名~' }}
        </el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-empty v-else description="未找到该同学" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getUser, getProfile } from '@/api/user'

const route = useRoute()
const loading = ref(false)
const user = ref(null)
const profile = ref(null)

function genderText(g) {
  return { MALE: '男', FEMALE: '女', UNKNOWN: '保密' }[g] || '保密'
}

onMounted(async () => {
  loading.value = true
  try {
    const uid = Number(route.params.id)
    const [userRes, profileRes] = await Promise.all([getUser(uid), getProfile(uid)])
    user.value = userRes.data
    profile.value = profileRes.data
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.back-btn {
  margin-bottom: 12px;
}

.profile-header {
  display: flex;
  align-items: center;
  gap: 20px;
}

.profile-header h2 {
  margin: 0 0 4px;
}

.tags {
  display: flex;
  gap: 8px;
  margin-top: 8px;
}
</style>
