<template>
  <div class="h5-page">
    <SakuraFall :density="14" />

    <div class="phone-frame">
      <!-- 主视觉 -->
      <section class="hero">
        <img class="hero-img" src="/images/h5-hero.png" alt="毕业纪念" />
        <div class="hero-mask" />
        <div class="hero-text">
          <p class="hero-sub">🌸 青春不散场</p>
          <h1 class="hero-title">{{ classInfo?.className || '大学同学录' }}</h1>
          <p class="hero-desc">{{ classInfo?.description || '记录我们一起走过的日子' }}</p>
        </div>
      </section>

      <!-- 毕业倒计时 -->
      <section v-if="classInfo?.daysToGraduation != null" class="countdown glass-card">
        <div class="cd-label">🎓 距离毕业还有</div>
        <div class="cd-num">
          <span class="num">{{ countdownText.num }}</span>
          <span class="unit">{{ countdownText.unit }}</span>
        </div>
        <div class="cd-date">{{ classInfo.graduationDate }}</div>
      </section>

      <!-- 成员 -->
      <section class="members glass-card">
        <h2 class="sec-title">✿ 班级成员</h2>
        <div v-for="m in members" :key="m.id" class="member-item" @click="$router.push(`/users/${m.userId}`)">
          <el-avatar :size="52" :src="m.avatar" class="avatar-ring">
            {{ (m.nickname || 'U').charAt(0) }}
          </el-avatar>
          <div class="member-info">
            <div class="m-name">
              {{ m.realName || m.nickname }}
              <span v-if="m.memberRole === 'OWNER'" class="owner-badge">班长</span>
            </div>
            <div class="m-motto">{{ m.motto || '还未留下签名' }}</div>
          </div>
          <div class="m-tags">
            <span v-if="m.constellation" class="anime-tag">{{ m.constellation }}</span>
            <span v-if="m.mbti" class="anime-tag">{{ m.mbti }}</span>
          </div>
        </div>
        <el-empty v-if="!members.length" description="还没有成员" :image-size="80" />
      </section>

      <!-- 生日提醒 -->
      <section v-if="birthdays.length" class="birthdays glass-card">
        <h2 class="sec-title">🎂 生日提醒</h2>
        <div v-for="b in birthdays.slice(0, 5)" :key="b.userId" class="b-item">
          <el-avatar :size="36" :src="b.avatar">{{ (b.nickname || 'U').charAt(0) }}</el-avatar>
          <span class="b-name">{{ b.nickname }}</span>
          <span class="b-const">{{ b.constellation }}</span>
          <span class="b-days" v-if="b.today">今天生日 🎉</span>
          <span class="b-days" v-else>{{ b.daysUntil }} 天后</span>
        </div>
      </section>

      <!-- 底部 -->
      <footer class="h5-footer">
        <p>🌸 樱花落下的速度是每秒五厘米</p>
        <p class="text-muted">大学同学录 · 记录青春，留住回忆</p>
        <el-button type="primary" round @click="$router.push('/classes')">打开完整版同学录</el-button>
      </footer>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { myClasses, getClassDetail, listMembers, birthdayReminders } from '@/api/class'
import SakuraFall from '@/components/SakuraFall.vue'

const classInfo = ref(null)
const members = ref([])
const birthdays = ref([])

const countdownText = computed(() => {
  const d = classInfo.value?.daysToGraduation
  if (d == null) return { num: '—', unit: '' }
  if (d < 0) return { num: Math.abs(d), unit: '天前毕业' }
  if (d === 0) return { num: '今天', unit: '毕业！' }
  return { num: d, unit: '天' }
})

onMounted(async () => {
  // 取用户加入的第一个班级作为展示对象
  const res = await myClasses()
  const first = res.data?.[0]
  if (!first) return
  const [detailRes, memberRes, birthdayRes] = await Promise.all([
    getClassDetail(first.id),
    listMembers(first.id),
    birthdayReminders(first.id, 365)
  ])
  classInfo.value = detailRes.data
  members.value = memberRes.data
  birthdays.value = birthdayRes.data || []
})
</script>

<style scoped>
.h5-page {
  min-height: 100vh;
  display: flex;
  justify-content: center;
  padding: 0;
}

/* 手机框：桌面端居中显示为手机宽度，移动端铺满 */
.phone-frame {
  position: relative;
  z-index: 2;
  width: 100%;
  max-width: 480px;
  background: rgba(255, 248, 251, 0.55);
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding-bottom: 16px;
}

@media (min-width: 481px) {
  .h5-page {
    padding: 24px 0;
  }
  .phone-frame {
    border-radius: 28px;
    overflow: hidden;
    box-shadow: 0 20px 60px rgba(251, 111, 146, 0.25);
    min-height: auto;
  }
}

.hero {
  position: relative;
  height: 380px;
  overflow: hidden;
}

.hero-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.hero-mask {
  position: absolute;
  inset: 0;
  background: linear-gradient(transparent 35%, rgba(74, 78, 105, 0.55));
}

.hero-text {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 24px;
  color: #fff;
}

.hero-sub {
  margin: 0 0 4px;
  font-size: 13px;
  letter-spacing: 3px;
  opacity: 0.95;
}

.hero-title {
  margin: 0 0 8px;
  font-size: 26px;
  letter-spacing: 1px;
  text-shadow: 0 2px 12px rgba(0, 0, 0, 0.3);
}

.hero-desc {
  margin: 0;
  font-size: 13px;
  opacity: 0.92;
}

.countdown,
.members,
.birthdays {
  margin: 0 16px;
}

.countdown {
  text-align: center;
}

.cd-label {
  font-size: 14px;
  color: var(--ink-light);
  letter-spacing: 2px;
}

.cd-num .num {
  font-size: 56px;
  font-weight: 800;
  background: linear-gradient(135deg, var(--sakura-primary-dark), #a08fd8);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}

.cd-num .unit {
  font-size: 16px;
  color: var(--ink);
  margin-left: 6px;
}

.cd-date {
  font-size: 12px;
  color: var(--ink-light);
}

.sec-title {
  margin: 0 0 16px;
  font-size: 18px;
  text-align: center;
  color: var(--ink);
}

.member-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.7);
  margin-bottom: 10px;
  cursor: pointer;
  transition: transform 0.2s;
}

.member-item:active {
  transform: scale(0.98);
}

.member-info {
  flex: 1;
  min-width: 0;
}

.m-name {
  font-weight: 700;
  font-size: 15px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.owner-badge {
  font-size: 11px;
  padding: 1px 8px;
  border-radius: 999px;
  background: #fff3d6;
  color: #c78d00;
  border: 1px solid #ffe1a3;
}

.m-motto {
  font-size: 12px;
  color: var(--ink-light);
  margin-top: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.m-tags {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 2px;
}

.b-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 4px;
  border-bottom: 1px dashed var(--sakura-light);
}

.b-item:last-child {
  border-bottom: none;
}

.b-name {
  font-weight: 600;
  flex: 1;
}

.b-const {
  font-size: 12px;
  color: var(--ink-light);
}

.b-days {
  font-size: 12px;
  color: var(--sakura-primary-dark);
  font-weight: 600;
}

.h5-footer {
  text-align: center;
  padding: 20px 16px 28px;
}

.h5-footer p {
  margin: 4px 0;
  font-size: 13px;
}

.h5-footer .el-button {
  margin-top: 14px;
}
</style>
