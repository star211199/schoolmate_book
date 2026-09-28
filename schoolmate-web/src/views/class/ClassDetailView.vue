<template>
  <div class="page-container" v-loading="loading">
    <div v-if="classInfo" class="class-hero glass-card">
      <div class="hero-banner">
        <div class="hero-left">
          <h2 class="anime-title">{{ classInfo.className }}</h2>
          <div class="tags">
            <span class="anime-tag">{{ classInfo.grade || '未填年级' }}</span>
            <span class="anime-tag">{{ classInfo.major || '未填专业' }}</span>
            <span class="anime-tag">{{ classInfo.memberCount }} 位成员</span>
          </div>
          <p class="desc text-muted">{{ classInfo.description || '这个班级还没有简介~' }}</p>
          <el-tooltip content="复制邀请码邀请同学加入" placement="top">
            <el-button plain size="small" @click="copyInviteCode">
              <el-icon><Link /></el-icon> 邀请码 {{ classInfo.inviteCode }}
            </el-button>
          </el-tooltip>
        </div>

        <!-- 毕业倒计时 -->
        <div v-if="classInfo.daysToGraduation != null" class="countdown-card">
          <div class="countdown-label">🎓 毕业倒计时</div>
          <div class="countdown-days">
            <span class="num">{{ countdownText.num }}</span>
            <span class="unit">{{ countdownText.unit }}</span>
          </div>
          <div class="countdown-date">{{ classInfo.graduationDate }}</div>
        </div>
      </div>

      <!-- 生日提醒 -->
      <div v-if="birthdays.length" class="birthday-bar">
        <span class="birthday-icon">🎂</span>
        <div v-for="b in birthdays.slice(0, 3)" :key="b.userId" class="birthday-item">
          <el-avatar :size="28" :src="b.avatar">{{ (b.nickname || 'U').charAt(0) }}</el-avatar>
          <span class="bname">{{ b.nickname }}</span>
          <span class="bdays" v-if="b.today">今天生日！🎉</span>
          <span class="bdays" v-else>{{ b.daysUntil }} 天后生日</span>
        </div>
      </div>
    </div>

    <el-card class="card-shadow tabs-card">
      <el-tabs v-model="activeTab">
        <!-- 成员 -->
        <el-tab-pane label="班级成员" name="members">
          <el-row :gutter="12">
            <el-col v-for="m in members" :key="m.id" :xs="12" :sm="8" :md="6">
              <div class="member-card" @click="$router.push(`/users/${m.userId}`)">
                <el-avatar :size="64" :src="m.avatar" class="avatar-ring">
                  {{ (m.nickname || 'U').charAt(0) }}
                </el-avatar>
                <div class="member-name">
                  {{ m.realName || m.nickname }}
                  <el-tag v-if="m.memberRole === 'OWNER'" size="small" type="warning" effect="plain">班长</el-tag>
                </div>
                <div class="member-motto">{{ m.motto || '还未留下签名' }}</div>
                <div class="member-tags">
                  <span v-if="m.constellation" class="anime-tag">{{ m.constellation }}</span>
                  <span v-if="m.mbti" class="anime-tag">{{ m.mbti }}</span>
                </div>
              </div>
            </el-col>
            <el-col v-if="members.length === 0" :span="24">
              <el-empty description="还没有成员" />
            </el-col>
          </el-row>
        </el-tab-pane>

        <!-- 留言板 -->
        <el-tab-pane label="留言板" name="messages">
          <div class="message-editor">
            <el-input
              v-model="messageContent"
              type="textarea"
              :rows="3"
              maxlength="1000"
              show-word-limit
              placeholder="留下你想说的话..."
            />
            <div class="editor-footer">
              <el-button type="primary" :loading="posting" @click="postMessage">发布留言</el-button>
            </div>
          </div>
          <el-divider />
          <div v-for="msg in messages" :key="msg.id" class="message-item">
            <el-avatar :size="40" :src="msg.avatar">
              {{ (msg.nickname || 'U').charAt(0) }}
            </el-avatar>
            <div class="message-body">
              <div class="message-head">
                <span class="nickname">{{ msg.nickname }}</span>
                <span class="text-muted">{{ formatTime(msg.createTime) }}</span>
                <el-button
                  v-if="canDelete(msg)"
                  link
                  type="danger"
                  size="small"
                  @click="removeMessage(msg.id)"
                >
                  删除
                </el-button>
              </div>
              <div class="message-content">{{ msg.content }}</div>
            </div>
          </div>
          <el-empty v-if="messages.length === 0" description="还没有留言，来说两句吧" />
          <div class="pagination">
            <el-pagination
              v-model:current-page="msgPage"
              :page-size="10"
              :total="msgTotal"
              layout="prev, pager, next"
              @current-change="loadMessages"
            />
          </div>
        </el-tab-pane>

        <!-- 相册 -->
        <el-tab-pane label="班级相册" name="albums">
          <div class="album-toolbar">
            <el-button type="primary" size="small" @click="albumDialog = true">
              <el-icon><Plus /></el-icon> 新建相册
            </el-button>
          </div>
          <el-row :gutter="12">
            <el-col v-for="a in albums" :key="a.id" :xs="12" :sm="8" :md="6">
              <el-card class="album-card" shadow="hover" @click="openAlbum(a)">
                <el-image :src="a.coverUrl || defaultCover" fit="cover" class="album-cover">
                  <template #error>
                    <div class="cover-placeholder"><el-icon><Picture /></el-icon></div>
                  </template>
                </el-image>
                <div class="album-name">{{ a.name }}</div>
                <div class="text-muted">{{ a.photoCount }} 张照片</div>
              </el-card>
            </el-col>
            <el-col v-if="albums.length === 0" :span="24">
              <el-empty description="还没有相册" />
            </el-col>
          </el-row>
        </el-tab-pane>

        <!-- 时间轴 -->
        <el-tab-pane label="班级动态" name="moments">
          <div class="message-editor">
            <el-input
              v-model="momentContent"
              type="textarea"
              :rows="3"
              maxlength="2000"
              show-word-limit
              placeholder="记录班级的新鲜事..."
            />
            <div class="editor-footer">
              <el-button type="primary" :loading="postingMoment" @click="postMoment">
                发布动态
              </el-button>
            </div>
          </div>
          <el-divider />
          <el-timeline>
            <el-timeline-item
              v-for="mm in moments"
              :key="mm.id"
              :timestamp="formatTime(mm.createTime)"
              placement="top"
              type="primary"
            >
              <el-card class="moment-card">
                <div class="moment-head">
                  <el-avatar :size="32" :src="mm.avatar">
                    {{ (mm.nickname || 'U').charAt(0) }}
                  </el-avatar>
                  <span class="nickname">{{ mm.nickname }}</span>
                </div>
                <div class="moment-content">{{ mm.content }}</div>
                <div class="moment-actions">
                  <el-button
                    link
                    size="small"
                    :class="{ 'like-btn': true, liked: mm.liked }"
                    @click="handleLike(mm)"
                  >
                    {{ mm.liked ? '❤️' : '🤍' }} {{ mm.likeCount || 0 }}
                  </el-button>
                  <el-button link size="small" @click="toggleComments(mm)">
                    <el-icon><ChatDotRound /></el-icon> 评论 {{ mm.commentCount }}
                  </el-button>
                  <el-button
                    v-if="canDelete(mm)"
                    link
                    type="danger"
                    size="small"
                    @click="removeMoment(mm.id)"
                  >
                    删除
                  </el-button>
                </div>

                <div v-if="mm.showComments" class="comment-area">
                  <div v-for="c in mm.comments || []" :key="c.id" class="comment-item">
                    <span class="comment-nick">{{ c.nickname }}：</span>
                    <span>{{ c.content }}</span>
                    <span class="text-muted">{{ formatTime(c.createTime) }}</span>
                  </div>
                  <div class="comment-input">
                    <el-input v-model="mm.newComment" size="small" placeholder="写评论..." />
                    <el-button size="small" type="primary" @click="postComment(mm)">发送</el-button>
                  </div>
                </div>
              </el-card>
            </el-timeline-item>
          </el-timeline>
          <el-empty v-if="moments.length === 0" description="还没有动态" />
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- 新建相册 -->
    <el-dialog v-model="albumDialog" title="新建相册" width="420px">
      <el-form label-width="80px">
        <el-form-item label="相册名称" required>
          <el-input v-model="albumForm.name" placeholder="如：毕业合影" />
        </el-form-item>
        <el-form-item label="相册描述">
          <el-input v-model="albumForm.description" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="albumDialog = false">取消</el-button>
        <el-button type="primary" @click="handleCreateAlbum">创建</el-button>
      </template>
    </el-dialog>

    <!-- 相册详情（照片 + 上传） -->
    <el-dialog v-model="photoDialog" :title="currentAlbum?.name || '相册'" width="800px">
      <div class="upload-area">
        <el-upload
          :show-file-list="false"
          :before-upload="handleUpload"
          accept="image/*"
          multiple
        >
          <el-button type="primary">
            <el-icon><Upload /></el-icon> 上传照片
          </el-button>
        </el-upload>
        <span class="text-muted">支持 jpg/png/gif/webp，单张不超过 10MB</span>
      </div>
      <el-row :gutter="12" style="margin-top: 16px">
        <el-col v-for="p in photos" :key="p.id" :xs="12" :sm="8" :md="6">
          <el-card class="photo-card" :body-style="{ padding: '8px' }">
            <el-image :src="p.url" fit="cover" class="photo-img" :preview-src-list="photoUrls" />
            <div class="photo-desc text-muted">{{ p.description || '—' }}</div>
            <el-button link type="danger" size="small" @click="removePhoto(p.id)">删除</el-button>
          </el-card>
        </el-col>
        <el-col v-if="photos.length === 0" :span="24">
          <el-empty description="相册还是空的" />
        </el-col>
      </el-row>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import {
  getClassDetail,
  listMembers,
  birthdayReminders,
  createClass
} from '@/api/class'
import { pageMessages, createMessage, deleteMessage } from '@/api/message'
import { listAlbums, createAlbum, listPhotos, uploadPhoto, deletePhoto } from '@/api/album'
import { pageMoments, createMoment, deleteMoment, listComments, createComment, toggleLike } from '@/api/moment'
import { ElMessage, ElMessageBox } from 'element-plus'

const route = useRoute()
const userStore = useUserStore()
const classId = computed(() => Number(route.params.id))

const loading = ref(false)
const activeTab = ref('members')
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

// 留言
const messages = ref([])
const messageContent = ref('')
const msgPage = ref(1)
const msgTotal = ref(0)
const posting = ref(false)

// 相册
const albums = ref([])
const albumDialog = ref(false)
const albumForm = ref({ name: '', description: '' })
const photoDialog = ref(false)
const currentAlbum = ref(null)
const photos = ref([])
const photoUrls = computed(() => photos.value.map((p) => p.url))

// 动态
const moments = ref([])
const momentContent = ref('')
const postingMoment = ref(false)

const defaultCover = '/images/album-sample.png'

function formatTime(t) {
  if (!t) return ''
  return String(t).replace('T', ' ').slice(0, 16)
}

function canDelete(item) {
  return item.userId === userStore.userId || userStore.isAdmin
}

async function loadAll() {
  loading.value = true
  try {
    const [detailRes, memberRes, albumRes, momentRes] = await Promise.all([
      getClassDetail(classId.value),
      listMembers(classId.value),
      listAlbums(classId.value),
      pageMoments(classId.value, { pageNum: 1, pageSize: 20 })
    ])
    classInfo.value = detailRes.data
    members.value = memberRes.data
    albums.value = albumRes.data
    moments.value = momentRes.data.records
    loadBirthdays()
    await loadMessages()
  } finally {
    loading.value = false
  }
}

async function loadMessages() {
  const res = await pageMessages(classId.value, { pageNum: msgPage.value, pageSize: 10 })
  messages.value = res.data.records
  msgTotal.value = res.data.total
}

async function postMessage() {
  if (!messageContent.value.trim()) {
    ElMessage.warning('请输入留言内容')
    return
  }
  posting.value = true
  try {
    await createMessage(classId.value, { content: messageContent.value })
    messageContent.value = ''
    ElMessage.success('发布成功')
    loadMessages()
  } finally {
    posting.value = false
  }
}

async function removeMessage(id) {
  await ElMessageBox.confirm('确定删除这条留言吗？', '提示', { type: 'warning' })
  await deleteMessage(id)
  ElMessage.success('已删除')
  loadMessages()
}

async function handleCreateAlbum() {
  if (!albumForm.value.name) {
    ElMessage.warning('请输入相册名称')
    return
  }
  await createAlbum(classId.value, albumForm.value)
  ElMessage.success('创建成功')
  albumDialog.value = false
  albumForm.value = { name: '', description: '' }
  const res = await listAlbums(classId.value)
  albums.value = res.data
}

async function openAlbum(album) {
  currentAlbum.value = album
  photoDialog.value = true
  const res = await listPhotos(album.id)
  photos.value = res.data
}

async function handleUpload(file) {
  const formData = new FormData()
  formData.append('file', file)
  await uploadPhoto(currentAlbum.value.id, formData)
  ElMessage.success('上传成功')
  const res = await listPhotos(currentAlbum.value.id)
  photos.value = res.data
  const albumsRes = await listAlbums(classId.value)
  albums.value = albumsRes.data
  return false
}

async function removePhoto(id) {
  await ElMessageBox.confirm('确定删除这张照片吗？', '提示', { type: 'warning' })
  await deletePhoto(id)
  ElMessage.success('已删除')
  const res = await listPhotos(currentAlbum.value.id)
  photos.value = res.data
}

async function postMoment() {
  if (!momentContent.value.trim()) {
    ElMessage.warning('请输入动态内容')
    return
  }
  postingMoment.value = true
  try {
    await createMoment(classId.value, { content: momentContent.value, imageUrls: [] })
    momentContent.value = ''
    ElMessage.success('发布成功')
    const res = await pageMoments(classId.value, { pageNum: 1, pageSize: 20 })
    moments.value = res.data.records
  } finally {
    postingMoment.value = false
  }
}

async function removeMoment(id) {
  await ElMessageBox.confirm('确定删除这条动态吗？', '提示', { type: 'warning' })
  await deleteMoment(id)
  ElMessage.success('已删除')
  const res = await pageMoments(classId.value, { pageNum: 1, pageSize: 20 })
  moments.value = res.data.records
}

async function toggleComments(moment) {
  if (moment.showComments) {
    moment.showComments = false
    return
  }
  const res = await listComments(moment.id)
  moment.comments = res.data
  moment.newComment = ''
  moment.showComments = true
}

async function postComment(moment) {
  if (!moment.newComment?.trim()) {
    ElMessage.warning('请输入评论内容')
    return
  }
  await createComment(moment.id, { content: moment.newComment })
  moment.newComment = ''
  const res = await listComments(moment.id)
  moment.comments = res.data
  moment.commentCount = res.data.length
}

async function loadBirthdays() {
  try {
    const res = await birthdayReminders(classInfo.value?.id ?? classId.value, 365)
    birthdays.value = res.data || []
  } catch (e) {
    birthdays.value = []
  }
}

async function handleLike(moment) {
  const res = await toggleLike(moment.id)
  moment.likeCount = res.data.likeCount
  moment.liked = res.data.liked
}

function copyInviteCode() {
  navigator.clipboard?.writeText(classInfo.value.inviteCode)
  ElMessage.success('邀请码已复制：' + classInfo.value.inviteCode)
}

onMounted(loadAll)
</script>

<style scoped>
.class-hero {
  margin-bottom: 16px;
  padding: 24px;
}

.hero-banner {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 24px;
  flex-wrap: wrap;
}

.hero-left h2 {
  margin: 0 0 10px;
  font-size: 24px;
}

.tags {
  display: flex;
  flex-wrap: wrap;
  margin-bottom: 10px;
}

.desc {
  margin: 0 0 12px;
}

.countdown-card {
  text-align: center;
  padding: 18px 32px;
  border-radius: 20px;
  background: linear-gradient(135deg, #ffe5ec, #eee8ff);
  border: 1px solid rgba(255, 255, 255, 0.9);
  box-shadow: 0 8px 24px rgba(251, 111, 146, 0.18);
}

.countdown-label {
  font-size: 13px;
  color: var(--ink-light);
  letter-spacing: 2px;
}

.countdown-days .num {
  font-size: 44px;
  font-weight: 800;
  background: linear-gradient(135deg, var(--sakura-primary-dark), #a08fd8);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}

.countdown-days .unit {
  font-size: 15px;
  color: var(--ink);
  margin-left: 4px;
}

.countdown-date {
  font-size: 12px;
  color: var(--ink-light);
}

.birthday-bar {
  display: flex;
  align-items: center;
  gap: 18px;
  flex-wrap: wrap;
  margin-top: 18px;
  padding: 10px 16px;
  border-radius: 14px;
  background: rgba(255, 229, 236, 0.6);
}

.birthday-icon {
  font-size: 20px;
}

.birthday-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.bname {
  font-size: 13px;
  font-weight: 600;
}

.bdays {
  font-size: 12px;
  color: var(--sakura-primary-dark);
}

.member-card {
  text-align: center;
  padding: 20px 12px;
  border-radius: 16px;
  margin-bottom: 12px;
  cursor: pointer;
  transition: all 0.25s;
  background: rgba(255, 255, 255, 0.65);
  border: 1px solid rgba(255, 255, 255, 0.9);
}

.member-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 28px rgba(251, 111, 146, 0.18);
  background: #fff;
}

.member-name {
  margin-top: 10px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}

.member-motto {
  font-size: 12px;
  color: var(--ink-light);
  margin: 6px 0 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.member-tags {
  min-height: 24px;
}

.message-editor,
.album-toolbar {
  margin-bottom: 8px;
}

.editor-footer {
  margin-top: 8px;
  text-align: right;
}

.message-item {
  display: flex;
  gap: 12px;
  padding: 12px 0;
  border-bottom: 1px solid #f0f0f0;
}

.message-body {
  flex: 1;
}

.message-head {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 4px;
}

.nickname {
  font-weight: 600;
}

.message-content {
  line-height: 1.7;
  white-space: pre-wrap;
}

.album-card {
  margin-bottom: 12px;
  cursor: pointer;
}

.album-cover {
  width: 100%;
  height: 120px;
  border-radius: 6px;
  display: block;
}

.cover-placeholder {
  height: 120px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  color: #c0c4cc;
  background: #f5f7fa;
}

.album-name {
  margin-top: 8px;
  font-weight: 600;
}

.moment-card {
  margin-bottom: 8px;
}

.moment-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.moment-content {
  line-height: 1.7;
  white-space: pre-wrap;
  margin-bottom: 8px;
}

.comment-area {
  margin-top: 8px;
  padding: 12px;
  background: #fafafa;
  border-radius: 6px;
}

.comment-item {
  font-size: 13px;
  line-height: 1.8;
  display: flex;
  gap: 6px;
}

.comment-nick {
  color: var(--el-color-primary);
}

.comment-input {
  display: flex;
  gap: 8px;
  margin-top: 8px;
}

.photo-img {
  width: 100%;
  height: 120px;
  border-radius: 4px;
  display: block;
}

.photo-desc {
  margin: 6px 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.upload-area {
  display: flex;
  align-items: center;
  gap: 12px;
}

.pagination {
  display: flex;
  justify-content: center;
  margin-top: 12px;
}

.like-btn {
  transition: transform 0.15s;
}

.like-btn:active {
  transform: scale(1.3);
}

.like-btn.liked {
  color: var(--sakura-primary-dark);
  font-weight: 700;
}
</style>
