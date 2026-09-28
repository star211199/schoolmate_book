<template>
  <div class="page-container" v-loading="loading">
    <el-button link @click="$router.back()" class="back-btn">
      <el-icon><ArrowLeft /></el-icon> 返回
    </el-button>

    <template v-if="user">
      <!-- 封面 + 资料卡 -->
      <div class="profile-hero glass-card">
        <div class="cover" :style="{ backgroundImage: `url(${profile?.coverImage || '/images/banner-graduation.png'})` }">
          <div class="cover-mask" />
        </div>
        <div class="hero-body">
          <el-avatar :size="104" :src="user.avatar" class="avatar-ring hero-avatar">
            {{ (user.nickname || 'U').charAt(0) }}
          </el-avatar>
          <div class="hero-info">
            <h1 class="anime-title">{{ profile?.realName || user.nickname }}</h1>
            <p class="motto">{{ profile?.motto || '这位同学很神秘，还没有留下签名~' }}</p>
            <div class="meta-tags">
              <span v-if="profile?.constellation" class="anime-tag">♓ {{ profile.constellation }}</span>
              <span v-if="profile?.mbti" class="anime-tag">{{ profile.mbti }}</span>
              <span v-if="profile?.enrollmentYear" class="anime-tag">{{ profile.enrollmentYear }} 级</span>
              <span v-if="profile?.currentCity" class="anime-tag">📍 {{ profile.currentCity }}</span>
              <span v-if="profile?.studentNo" class="anime-tag">学号 {{ profile.studentNo }}</span>
            </div>
          </div>
        </div>
      </div>

      <div class="profile-grid">
        <!-- 左列：基础信息 + 社交 -->
        <div class="col">
          <div class="glass-card block">
            <h3 class="block-title">📇 基本资料</h3>
            <div class="info-list">
              <div class="info-item"><span class="label">昵称</span><span>{{ user.nickname }}</span></div>
              <div class="info-item"><span class="label">性别</span><span>{{ genderText(profile?.gender) }}</span></div>
              <div class="info-item"><span class="label">生日</span><span>{{ profile?.birthday || '—' }}</span></div>
              <div class="info-item"><span class="label">籍贯</span><span>{{ profile?.hometown || '—' }}</span></div>
              <div class="info-item"><span class="label">联系方式</span><span>{{ profile?.contact || '—' }}</span></div>
              <div class="info-item"><span class="label">入学年份</span><span>{{ profile?.enrollmentYear || '—' }}</span></div>
            </div>
          </div>

          <div v-if="hasSocial" class="glass-card block">
            <h3 class="block-title">🔗 找到 TA</h3>
            <div class="social-list">
              <div v-if="profile?.socialLinks?.qq" class="social-item">🐧 QQ：{{ profile.socialLinks.qq }}</div>
              <div v-if="profile?.socialLinks?.wechat" class="social-item">💬 微信：{{ profile.socialLinks.wechat }}</div>
              <div v-if="profile?.socialLinks?.github" class="social-item">
                🐙 GitHub：{{ profile.socialLinks.github }}
              </div>
              <div v-if="profile?.socialLinks?.weibo" class="social-item">🌊 微博：{{ profile.socialLinks.weibo }}</div>
            </div>
          </div>
        </div>

        <!-- 右列：兴趣技能 + 毕业寄语 -->
        <div class="col">
          <div class="glass-card block">
            <h3 class="block-title">🎨 兴趣爱好</h3>
            <div v-if="profile?.hobbies?.length" class="tag-wall">
              <span v-for="h in profile.hobbies" :key="h" class="anime-tag big">{{ h }}</span>
            </div>
            <p v-else class="text-muted">还没有填写兴趣爱好~</p>
          </div>

          <div class="glass-card block">
            <h3 class="block-title">⚡ 技能标签</h3>
            <div v-if="profile?.skills?.length" class="tag-wall">
              <span v-for="s in profile.skills" :key="s" class="anime-tag big skill">{{ s }}</span>
            </div>
            <p v-else class="text-muted">还没有填写技能标签~</p>
          </div>

          <div class="glass-card block message-block">
            <h3 class="block-title">🌸 毕业寄语</h3>
            <p class="graduation-message">
              {{ profile?.graduationMessage || '愿历经千帆，归来仍是少年。' }}
            </p>
          </div>
        </div>
      </div>
    </template>

    <el-empty v-else description="未找到该同学" />
  </div>
</template>

<script setup>
import { computed, ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getUser, getProfile } from '@/api/user'

const route = useRoute()
const loading = ref(false)
const user = ref(null)
const profile = ref(null)

const hasSocial = computed(() => {
  const s = profile.value?.socialLinks
  return s && Object.values(s).some(Boolean)
})

function genderText(g) {
  return { MALE: '男', FEMALE: '女', UNKNOWN: '保密' }[g] || g || '保密'
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

.profile-hero {
  position: relative;
  padding: 0;
  overflow: hidden;
}

.cover {
  height: 200px;
  background-size: cover;
  background-position: center;
  position: relative;
}

.cover-mask {
  position: absolute;
  inset: 0;
  background: linear-gradient(transparent 30%, rgba(255, 255, 255, 0.9));
}

.hero-body {
  display: flex;
  align-items: flex-end;
  gap: 20px;
  padding: 0 32px 24px;
  margin-top: -52px;
  position: relative;
}

.hero-avatar {
  flex-shrink: 0;
  background: var(--el-color-primary-light-8);
}

.hero-info h1 {
  margin: 0 0 6px;
  font-size: 26px;
}

.motto {
  margin: 0 0 10px;
  color: var(--ink-light);
  font-size: 14px;
}

.meta-tags {
  display: flex;
  flex-wrap: wrap;
}

.profile-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
  margin-top: 20px;
}

.col {
  display: flex;
  flex-direction: column;
  gap: 20px;
  min-width: 0;
}

.block-title {
  margin: 0 0 16px;
  font-size: 16px;
  color: var(--ink);
}

.info-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.info-item {
  display: flex;
  font-size: 14px;
}

.info-item .label {
  width: 84px;
  color: var(--ink-light);
  flex-shrink: 0;
}

.social-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  font-size: 14px;
}

.tag-wall {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.anime-tag.big {
  font-size: 13px;
  padding: 5px 14px;
}

.anime-tag.skill {
  background: linear-gradient(135deg, #eee8ff, #e0f2ff);
  border-color: var(--lavender);
  color: #6d5fc0;
}

.message-block {
  background: linear-gradient(135deg, rgba(255, 229, 236, 0.85), rgba(238, 232, 255, 0.85));
}

.graduation-message {
  margin: 0;
  font-size: 15px;
  line-height: 1.9;
  color: var(--ink);
  font-style: italic;
}

@media (max-width: 768px) {
  .profile-grid {
    grid-template-columns: 1fr;
  }
  .hero-body {
    flex-direction: column;
    align-items: center;
    text-align: center;
    margin-top: -52px;
  }
}
</style>
