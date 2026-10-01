<template>
  <div class="page-container notification-page">
    <!-- 头部：标题 + 未读统计 + 全部已读 -->
    <div class="glass-card head-card">
      <div class="head-left">
        <h2 class="anime-title">通知中心</h2>
        <p class="text-muted">好友申请、点赞评论、时光胶囊到期都会在这里提醒你</p>
      </div>
      <div class="head-right">
        <div class="head-stat">
          <b :class="{ warn: store.unreadCount > 0 }">{{ store.unreadCount }}</b>
          <span>未读</span>
        </div>
        <el-button
          :disabled="!store.unreadCount"
          :loading="markingAll"
          @click="handleMarkAll"
        >
          全部已读
        </el-button>
      </div>
    </div>

    <!-- 列表 -->
    <div class="glass-card body-card">
      <div class="toolbar">
        <el-radio-group v-model="filter" size="small" @change="reload">
          <el-radio-button :value="'all'">全部</el-radio-button>
          <el-radio-button :value="'unread'">未读</el-radio-button>
        </el-radio-group>
        <el-button size="small" text :loading="loading" @click="reload">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
      </div>

      <div v-loading="loading" class="notif-list">
        <div
          v-for="n in list"
          :key="n.id"
          class="notif-item"
          :class="{ unread: !n.read }"
          @click="handleClick(n)"
        >
          <div class="notif-avatar">
            <img v-if="n.fromAvatar" :src="n.fromAvatar" alt="" />
            <span v-else class="glyph">{{ typeGlyph(n.type) }}</span>
            <i class="type-dot" :class="typeClass(n.type)">{{ typeGlyph(n.type) }}</i>
          </div>

          <div class="notif-main">
            <div class="notif-title">
              {{ n.title }}
              <span v-if="!n.read" class="dot" />
            </div>
            <div v-if="n.content" class="notif-content">{{ n.content }}</div>
            <div class="notif-meta">
              <span>{{ formatTime(n.createTime) }}</span>
              <span class="biz">{{ bizLabel(n.bizType) }}</span>
            </div>
          </div>

          <el-icon class="go"><ArrowRight /></el-icon>
        </div>

        <el-empty
          v-if="!loading && !list.length"
          :description="filter === 'unread' ? '没有未读通知' : '还没有收到通知'"
          :image-size="90"
        />
      </div>

      <div v-if="hasMore && !loading" class="load-more">
        <el-button text @click="loadMore">加载更多</el-button>
      </div>
      <div v-else-if="list.length && !loading" class="load-more text-muted">— 没有更多了 —</div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Refresh, ArrowRight } from '@element-plus/icons-vue'
import { useNotificationStore } from '@/stores/notification'
import {
  getNotifications,
  markNotificationRead,
  markAllNotificationsRead
} from '@/api/notification'

const router = useRouter()
const store = useNotificationStore()

const list = ref([])
const loading = ref(false)
const markingAll = ref(false)
const filter = ref('all')
const pageNum = ref(1)
const pageSize = 15
const total = ref(0)

const hasMore = computed(() => list.value.length < total.value)

/** 类型 → 图标字符（用文字符号避免再引入一堆图标组件） */
function typeGlyph(type) {
  switch (type) {
    case 'FRIEND_REQUEST':
    case 'FRIEND_ACCEPTED':
      return '友'
    case 'MOMENT_LIKE':
      return '赞'
    case 'MOMENT_COMMENT':
      return '评'
    case 'CAPSULE_OPENED':
      return '信'
    default:
      return '通'
  }
}

/** 类型 → 小圆点配色（走主题语义色，不写死色值） */
function typeClass(type) {
  switch (type) {
    case 'FRIEND_REQUEST':
      return 't-friend'
    case 'FRIEND_ACCEPTED':
      return 't-friend'
    case 'MOMENT_LIKE':
      return 't-like'
    case 'MOMENT_COMMENT':
      return 't-comment'
    case 'CAPSULE_OPENED':
      return 't-capsule'
    default:
      return 't-default'
  }
}

function bizLabel(bizType) {
  switch (bizType) {
    case 'FRIEND':
      return '好友'
    case 'MOMENT':
      return '班级动态'
    case 'CAPSULE':
      return '时光胶囊'
    default:
      return ''
  }
}

function formatTime(t) {
  if (!t) return ''
  // 后端返回 yyyy-MM-dd HH:mm:ss
  const d = new Date(String(t).replace(' ', 'T'))
  if (Number.isNaN(d.getTime())) return String(t)
  const diff = Date.now() - d.getTime()
  if (diff < 60 * 1000) return '刚刚'
  if (diff < 60 * 60 * 1000) return `${Math.floor(diff / 60000)} 分钟前`
  if (diff < 24 * 60 * 60 * 1000) return `${Math.floor(diff / 3600000)} 小时前`
  if (diff < 7 * 24 * 60 * 60 * 1000) return `${Math.floor(diff / 86400000)} 天前`
  const p = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}`
}

async function load(reset = true) {
  if (loading.value) return
  loading.value = true
  if (reset) {
    pageNum.value = 1
  }
  try {
    const res = await getNotifications({
      pageNum: pageNum.value,
      pageSize,
      unreadOnly: filter.value === 'unread' ? true : undefined
    })
    const data = res.data || {}
    const records = data.records || []
    list.value = reset ? records : list.value.concat(records)
    total.value = Number(data.total || 0)
  } catch {
    // 拦截器已提示
  } finally {
    loading.value = false
  }
}

function reload() {
  load(true)
}

function loadMore() {
  if (!hasMore()) return
  pageNum.value += 1
  load(false)
}

/** 点击通知：先标已读，再按业务类型跳转 */
async function handleClick(n) {
  if (!n.read) {
    try {
      await markNotificationRead(n.id)
      n.read = true
      store.decrease(1)
    } catch {
      // 标已读失败不阻断跳转
    }
  }
  goto(n)
}

function goto(n) {
  switch (n.bizType) {
    case 'FRIEND':
      router.push('/contacts')
      break
    case 'MOMENT':
      // bizId 存的是 classId，直接进班级主页的动态 tab
      if (n.bizId) {
        router.push({ path: `/classes/${n.bizId}`, query: { tab: 'moments' } })
      } else {
        router.push('/classes')
      }
      break
    case 'CAPSULE':
      router.push('/capsules')
      break
    default:
      break
  }
}

async function handleMarkAll() {
  markingAll.value = true
  try {
    await markAllNotificationsRead()
    ElMessage.success('已全部标记为已读')
    store.reset()
    reload()
  } catch {
    // 拦截器已提示
  } finally {
    markingAll.value = false
  }
}

onMounted(() => {
  load(true)
  // 进来就算「看过一眼」，把角标同步成真实值
  store.refreshUnread()
})
</script>

<style scoped>
.head-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  flex-wrap: wrap;
  margin-bottom: 16px;
}

.head-left h2 {
  margin: 0 0 6px;
  font-size: 22px;
}

.head-left p {
  margin: 0;
}

.head-right {
  display: flex;
  align-items: center;
  gap: 18px;
}

.head-stat {
  text-align: center;
}

.head-stat b {
  display: block;
  font-size: 24px;
  line-height: 1.1;
  color: var(--color-text);
}

.head-stat b.warn {
  color: var(--color-brand-dark);
}

.head-stat span {
  font-size: 12px;
  color: var(--color-text-muted);
}

.body-card {
  min-height: 320px;
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.notif-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-height: 120px;
}

.notif-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 16px;
  border-radius: 14px;
  border: 1px solid transparent;
  background: var(--color-glass);
  cursor: pointer;
  transition: transform 0.18s ease, box-shadow 0.18s ease, border-color 0.18s ease;
}

.notif-item:hover {
  transform: translateX(2px);
  border-color: var(--color-brand-light);
  box-shadow: 0 6px 20px var(--color-shadow);
}

.notif-item.unread {
  background: var(--color-brand-lighter);
}

.notif-avatar {
  position: relative;
  width: 42px;
  height: 42px;
  flex: none;
  border-radius: 50%;
  overflow: visible;
}

.notif-avatar img,
.notif-avatar .glyph {
  width: 42px;
  height: 42px;
  border-radius: 50%;
  object-fit: cover;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--color-brand-light);
  color: #fff;
  font-size: 16px;
}

.type-dot {
  position: absolute;
  right: -3px;
  bottom: -3px;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  font-size: 11px;
  font-style: normal;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  border: 2px solid var(--color-surface);
  background: var(--color-text-subtle);
}

.type-dot.t-friend {
  background: var(--color-brand-dark);
}

.type-dot.t-like {
  background: var(--color-accent);
}

.type-dot.t-comment {
  background: var(--color-brand);
}

.type-dot.t-capsule {
  background: var(--color-online);
}

.notif-main {
  flex: 1;
  min-width: 0;
}

.notif-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text);
}

.notif-title .dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--color-brand-dark);
  flex: none;
}

.notif-content {
  margin-top: 4px;
  font-size: 13px;
  color: var(--color-text-muted);
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.notif-meta {
  margin-top: 6px;
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 12px;
  color: var(--color-text-subtle);
}

.notif-meta .biz {
  padding: 1px 8px;
  border-radius: 999px;
  background: var(--color-brand-lighter);
  color: var(--color-brand-dark);
}

.go {
  color: var(--color-text-subtle);
  flex: none;
}

.load-more {
  text-align: center;
  padding-top: 14px;
}
</style>
