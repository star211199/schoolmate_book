<template>
  <div class="chat-page">
    <!-- ============ 左：会话列表 ============ -->
    <aside class="session-panel glass-card" :class="{ 'mobile-hide': isMobile && activeSessionId }">
      <div class="panel-head">
        <div class="panel-title">
          <span>消息</span>
          <span class="conn-state" :class="{ on: chatStore.connected }">
            {{ chatStore.connected ? '已连接' : '连接中...' }}
          </span>
        </div>
        <div class="panel-actions">
          <el-tooltip content="发起群聊" placement="bottom">
            <button class="icon-btn" @click="openGroupDialog">
              <el-icon><ChatDotRound /></el-icon>
            </button>
          </el-tooltip>
          <el-tooltip content="找同学聊天" placement="bottom">
            <button class="icon-btn" @click="router.push('/contacts')">
              <el-icon><Search /></el-icon>
            </button>
          </el-tooltip>
        </div>
      </div>

      <div class="session-list" v-loading="chatStore.loadingSessions">
        <div
          v-for="s in chatStore.sessions"
          :key="s.id"
          class="session-item"
          :class="{ active: s.id === chatStore.activeSessionId }"
          @click="openSession(s.id)"
        >
          <div class="s-avatar" :class="{ group: s.sessionType === 'GROUP' }">
            <img v-if="s.avatar" :src="s.avatar" alt="" />
            <span v-else>{{ (s.title || '?').slice(0, 1) }}</span>
            <i v-if="s.sessionType === 'PRIVATE' && s.online" class="online-dot"></i>
          </div>
          <div class="s-main">
            <div class="s-top">
              <span class="s-title">
                {{ s.title || '未命名会话' }}
                <em v-if="s.sessionType === 'GROUP'">({{ s.memberCount || 0 }})</em>
              </span>
              <span class="s-time">{{ shortTime(s.lastMessageTime) }}</span>
            </div>
            <div class="s-bottom">
              <span class="s-preview">{{ s.lastMessagePreview || '暂无消息' }}</span>
              <span v-if="s.unreadCount" class="unread-badge">{{ s.unreadCount > 99 ? '99+' : s.unreadCount }}</span>
            </div>
          </div>
        </div>

        <el-empty
          v-if="!chatStore.loadingSessions && !chatStore.sessions.length"
          description="还没有会话，去联系人里找同学聊聊吧"
          :image-size="80"
        />
      </div>
    </aside>

    <!-- ============ 右：聊天窗口 ============ -->
    <section class="chat-panel glass-card" :class="{ 'mobile-hide': isMobile && !activeSessionId }">
      <template v-if="current">
        <header class="chat-head">
          <button v-if="isMobile" class="icon-btn back" @click="chatStore.activeSessionId = null">
            <el-icon><ArrowLeft /></el-icon>
          </button>
          <div class="s-avatar" :class="{ group: current.sessionType === 'GROUP' }">
            <img v-if="current.avatar" :src="current.avatar" alt="" />
            <span v-else>{{ (current.title || '?').slice(0, 1) }}</span>
          </div>
          <div class="chat-head-info">
            <div class="chat-title">{{ current.title }}</div>
            <div class="chat-sub">
              <template v-if="current.sessionType === 'GROUP'">
                {{ current.memberCount }} 位成员
                <span v-if="current.ownerName"> · 群主 {{ current.ownerName }}</span>
              </template>
              <template v-else>
                <span :class="current.online ? 'is-online' : 'is-offline'">
                  {{ current.online ? '在线' : '离线' }}
                </span>
              </template>
            </div>
          </div>
          <el-button
            v-if="current.sessionType === 'GROUP'"
            text
            size="small"
            @click="openGroupDetail"
          >
            群资料
          </el-button>
        </header>

        <div ref="scrollRef" class="message-list" @scroll="onScroll">
          <div v-if="hasMore" class="load-more">
            <el-button size="small" text @click="loadMore">查看更早的消息</el-button>
          </div>

          <template v-for="(m, i) in messageList" :key="m.id || m.clientMsgId || i">
            <!-- 系统提示：居中灰条 -->
            <div v-if="m.msgType === 'SYSTEM'" class="sys-tip">{{ m.content }}</div>
            <div v-else-if="m.msgType === 'RECALL'" class="sys-tip">对方撤回了一条消息</div>

            <div v-else class="msg-row" :class="{ me: isMine(m) }">
              <div class="m-avatar">
                <img v-if="m.senderAvatar" :src="m.senderAvatar" alt="" />
                <span v-else>{{ (senderName(m) || '?').slice(0, 1) }}</span>
              </div>
              <div class="m-body">
                <div v-if="current.sessionType === 'GROUP' && !isMine(m)" class="m-name">
                  {{ senderName(m) }}
                </div>
                <div class="bubble" :class="{ failed: m._failed }" v-html="m.content"></div>
                <div class="m-meta">
                  <span>{{ shortTime(m.sendTime) }}</span>
                  <span v-if="m._pending" class="pending">发送中</span>
                  <span v-else-if="m._failed" class="failed-tip" @click="chatStore.resendMessage(m)">发送失败，点击重试</span>
                </div>
              </div>
            </div>
          </template>
        </div>

        <footer class="composer">
          <textarea
            v-model="draft"
            class="composer-input"
            placeholder="输入消息，Enter 发送，Shift + Enter 换行"
            @keydown.enter.exact.prevent="send"
          ></textarea>
          <el-button type="primary" class="send-btn" :disabled="!draft.trim()" @click="send">
            发送
          </el-button>
        </footer>
      </template>

      <el-empty v-else description="选择左侧的一个会话开始聊天" :image-size="100" />
    </section>

    <!-- 发起群聊 -->
    <el-dialog v-model="groupDialog" title="发起群聊" width="440px">
      <el-form label-width="72px">
        <el-form-item label="群名称">
          <el-input v-model="groupForm.groupName" placeholder="例如：3 班考研小队" maxlength="30" />
        </el-form-item>
        <el-form-item label="选择成员">
          <div class="member-picker">
            <el-checkbox-group v-model="groupForm.memberIds">
              <el-checkbox v-for="f in friends" :key="f.userId" :value="f.userId" class="picker-item">
                {{ f.displayName }}
              </el-checkbox>
            </el-checkbox-group>
            <el-empty v-if="!friends.length" description="还没有好友，先去添加吧" :image-size="60" />
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="groupDialog = false">取消</el-button>
        <el-button type="primary" :loading="creatingGroup" @click="submitGroup">创建</el-button>
      </template>
    </el-dialog>

    <!-- 群资料 -->
    <el-drawer v-model="groupDetailDrawer" title="群资料" size="380px">
      <div v-if="groupDetail" class="group-detail">
        <div class="gd-head">
          <div class="s-avatar group lg">
            <span>{{ (groupDetail.groupName || '?').slice(0, 1) }}</span>
          </div>
          <div>
            <div class="gd-name">{{ groupDetail.groupName }}</div>
            <div class="gd-sub">
              {{ groupDetail.memberCount }} 位成员 · 我的身份 {{ roleText(groupDetail.myRole) }}
            </div>
          </div>
        </div>

        <div class="gd-block">
          <div class="gd-label">群公告</div>
          <div class="gd-value">{{ groupDetail.notice || '暂无公告' }}</div>
        </div>

        <div class="gd-block">
          <div class="gd-label">成员</div>
          <div class="gd-members">
            <div v-for="m in groupMembers" :key="m.userId" class="gd-member">
              <div class="s-avatar sm">
                <img v-if="m.avatar" :src="m.avatar" alt="" />
                <span v-else>{{ (m.nickname || '?').slice(0, 1) }}</span>
              </div>
              <span class="gd-member-name">{{ m.groupNickname || m.nickname }}</span>
              <span v-if="m.memberRole === 'OWNER'" class="tag-owner">群主</span>
              <span v-else-if="m.memberRole === 'ADMIN'" class="tag-admin">管理员</span>
            </div>
          </div>
        </div>

        <div class="gd-actions">
          <el-button v-if="groupDetail.groupType !== 'CLASS'" type="danger" plain @click="onQuitGroup">
            退出群聊
          </el-button>
          <el-button v-else disabled plain>班级群不可退出，请先退出班级</el-button>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, computed, watch, nextTick, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useChatStore } from '@/stores/chat'
import { useUserStore } from '@/stores/user'
import { getFriends } from '@/api/friend'
import { createGroup, getGroupDetail, getGroupMembers, quitGroup } from '@/api/group'

const router = useRouter()
const chatStore = useChatStore()
const userStore = useUserStore()

const scrollRef = ref(null)
const draft = ref('')
const hasMore = ref(true)
const isMobile = ref(window.innerWidth < 820)

const groupDialog = ref(false)
const groupForm = ref({ groupName: '', memberIds: [] })
const creatingGroup = ref(false)
const friends = ref([])

const groupDetailDrawer = ref(false)
const groupDetail = ref(null)
const groupMembers = ref([])

const current = computed(() => chatStore.activeSession)
const messageList = computed(() => chatStore.activeMessages())

function isMine(m) {
  return m.senderId === userStore.userId
}

function senderName(m) {
  return m.senderDisplayName || m.senderNickname || '同学'
}

function roleText(role) {
  return { OWNER: '群主', ADMIN: '管理员', MEMBER: '成员' }[role] || '成员'
}

/** 会话列表用：今天显示时间，更早显示日期 */
function shortTime(value) {
  if (!value) return ''
  const text = String(value).replace('T', ' ')
  const today = new Date()
  const p = (n) => String(n).padStart(2, '0')
  const todayPrefix = `${today.getFullYear()}-${p(today.getMonth() + 1)}-${p(today.getDate())}`
  if (text.startsWith(todayPrefix)) {
    return text.slice(11, 16)
  }
  return text.slice(5, 10)
}

function scrollToBottom() {
  nextTick(() => {
    const el = scrollRef.value
    if (el) {
      el.scrollTop = el.scrollHeight
    }
  })
}

async function openSession(sessionId) {
  await chatStore.openSession(sessionId)
  hasMore.value = true
  scrollToBottom()
}

async function loadMore() {
  const sid = chatStore.activeSessionId
  if (!sid) return
  const el = scrollRef.value
  const beforeHeight = el ? el.scrollHeight : 0
  const list = await chatStore.loadMore(sid)
  hasMore.value = list.length > 0
  nextTick(() => {
    // 加载更多后保持视觉位置，避免「跳一下」
    if (el) {
      el.scrollTop = el.scrollHeight - beforeHeight
    }
  })
}

function onScroll() {
  if (scrollRef.value && scrollRef.value.scrollTop < 30) {
    /* 提示用户可点击上方按钮加载更多，避免自动触发打乱阅读位置 */
  }
}

function send() {
  const content = draft.value.trim()
  if (!content || !chatStore.activeSessionId) return
  chatStore.sendMessage(chatStore.activeSessionId, content)
  draft.value = ''
  scrollToBottom()
}

async function openGroupDialog() {
  groupForm.value = { groupName: '', memberIds: [] }
  const res = await getFriends()
  friends.value = res.data || []
  groupDialog.value = true
}

async function submitGroup() {
  if (!groupForm.value.groupName.trim()) {
    ElMessage.warning('请填写群名称')
    return
  }
  creatingGroup.value = true
  try {
    const res = await createGroup({
      groupName: groupForm.value.groupName.trim(),
      memberIds: groupForm.value.memberIds
    })
    ElMessage.success('群创建成功')
    groupDialog.value = false
    await chatStore.loadSessions()
    await openSession(res.data)
  } finally {
    creatingGroup.value = false
  }
}

async function openGroupDetail() {
  const groupId = current.value?.groupId
  if (!groupId) return
  const [detail, members] = await Promise.all([
    getGroupDetail(groupId),
    getGroupMembers(groupId, 1, 100)
  ])
  groupDetail.value = detail.data
  groupMembers.value = members.data?.records || []
  groupDetailDrawer.value = true
}

async function onQuitGroup() {
  await ElMessageBox.confirm('退出后将不再接收该群消息，确定退出吗？', '退出群聊', {
    type: 'warning'
  })
  await quitGroup(groupDetail.value.id)
  ElMessage.success('已退出群聊')
  groupDetailDrawer.value = false
  chatStore.activeSessionId = null
  await chatStore.loadSessions()
}

/** 新消息到达时自动滚到底部 */
watch(
  () => messageList.value.length,
  () => scrollToBottom()
)

function onResize() {
  isMobile.value = window.innerWidth < 820
}

onMounted(async () => {
  window.addEventListener('resize', onResize)
  if (!chatStore.sessions.length) {
    await chatStore.loadSessions()
  }
  // 从联系人页跳转过来时带上 sessionId
  const querySessionId = Number(router.currentRoute.value.query.sessionId)
  if (querySessionId) {
    await openSession(querySessionId)
  }
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', onResize)
})
</script>

<style scoped>
.chat-page {
  display: grid;
  grid-template-columns: 320px 1fr;
  gap: 16px;
  height: calc(100vh - 150px);
  min-height: 520px;
}

@media (max-width: 820px) {
  .chat-page {
    grid-template-columns: 1fr;
    height: calc(100vh - 130px);
  }
  .mobile-hide {
    display: none;
  }
}

/* ---------- 会话列表 ---------- */
.session-panel {
  display: flex;
  flex-direction: column;
  padding: 0;
  overflow: hidden;
}

.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 16px 12px;
  border-bottom: 1px solid var(--color-border);
}

.panel-title {
  display: flex;
  align-items: baseline;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text);
}

.conn-state {
  font-size: 11px;
  color: var(--color-text-subtle);
}
.conn-state.on {
  color: var(--color-online);
}

.panel-actions {
  display: flex;
  gap: 6px;
}

.icon-btn {
  width: 32px;
  height: 32px;
  border: none;
  border-radius: 10px;
  background: var(--color-surface-2);
  color: var(--color-text-muted);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.16s;
}
.icon-btn:hover {
  background: var(--color-brand-lighter);
  color: var(--color-brand-dark);
}
.icon-btn.back {
  margin-right: 4px;
}

.session-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.session-item {
  display: flex;
  gap: 11px;
  padding: 10px 11px;
  border-radius: 12px;
  cursor: pointer;
  transition: background 0.16s;
}
.session-item:hover {
  background: var(--color-surface-2);
}
.session-item.active {
  background: var(--color-brand-lighter);
}

.s-avatar {
  position: relative;
  width: 42px;
  height: 42px;
  border-radius: 13px;
  flex: none;
  background: var(--color-brand-light);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  font-weight: 600;
  overflow: visible;
}
.s-avatar img {
  width: 100%;
  height: 100%;
  border-radius: 13px;
  object-fit: cover;
}
.s-avatar.group {
  background: var(--color-accent);
}
.s-avatar.sm {
  width: 30px;
  height: 30px;
  font-size: 12px;
  border-radius: 9px;
}
.s-avatar.sm img {
  border-radius: 9px;
}
.s-avatar.lg {
  width: 56px;
  height: 56px;
  font-size: 22px;
  border-radius: 17px;
}
.s-avatar .online-dot {
  position: absolute;
  right: -2px;
  bottom: -2px;
}

.s-main {
  flex: 1;
  min-width: 0;
}
.s-top {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 8px;
}
.s-title {
  font-size: 14px;
  font-weight: 500;
  color: var(--color-text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.s-title em {
  font-style: normal;
  font-size: 12px;
  color: var(--color-text-subtle);
}
.s-time {
  font-size: 11px;
  color: var(--color-text-subtle);
  flex: none;
}
.s-bottom {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-top: 2px;
}
.s-preview {
  font-size: 12.5px;
  color: var(--color-text-muted);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
}

/* ---------- 聊天窗口 ---------- */
.chat-panel {
  display: flex;
  flex-direction: column;
  padding: 0;
  overflow: hidden;
}

.chat-head {
  display: flex;
  align-items: center;
  gap: 11px;
  padding: 13px 18px;
  border-bottom: 1px solid var(--color-border);
}
.chat-head-info {
  flex: 1;
  min-width: 0;
}
.chat-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text);
}
.chat-sub {
  font-size: 12px;
  color: var(--color-text-muted);
}
.is-online {
  color: var(--color-online);
}
.is-offline {
  color: var(--color-text-subtle);
}

.message-list {
  flex: 1;
  overflow-y: auto;
  padding: 18px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.load-more {
  text-align: center;
  padding-bottom: 6px;
}

.sys-tip {
  align-self: center;
  font-size: 11.5px;
  color: var(--color-text-subtle);
  background: var(--color-surface-2);
  padding: 3px 12px;
  border-radius: 999px;
  max-width: 80%;
  text-align: center;
}

.msg-row {
  display: flex;
  gap: 9px;
  max-width: 76%;
}
.msg-row.me {
  margin-left: auto;
  flex-direction: row-reverse;
}

.m-avatar {
  width: 34px;
  height: 34px;
  border-radius: 11px;
  flex: none;
  background: var(--color-accent-light);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 600;
  overflow: hidden;
}
.m-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.m-body {
  min-width: 0;
}
.m-name {
  font-size: 11.5px;
  color: var(--color-text-subtle);
  margin-bottom: 3px;
}

.bubble {
  padding: 9px 13px;
  border-radius: 14px;
  font-size: 13.5px;
  line-height: 1.6;
  word-break: break-word;
  white-space: pre-wrap;
  background: var(--color-bubble-other);
  color: var(--color-bubble-other-fg);
  border-bottom-left-radius: 4px;
  border: 1px solid var(--color-border);
}
.msg-row.me .bubble {
  background: var(--color-bubble-me);
  color: var(--color-bubble-me-fg);
  border-color: transparent;
  border-bottom-left-radius: 14px;
  border-bottom-right-radius: 4px;
}
.bubble.failed {
  opacity: 0.65;
}

.m-meta {
  display: flex;
  gap: 8px;
  align-items: center;
  font-size: 11px;
  color: var(--color-text-subtle);
  margin-top: 3px;
}
.msg-row.me .m-meta {
  justify-content: flex-end;
}
.pending {
  color: var(--color-text-subtle);
}
.failed-tip {
  color: var(--color-danger);
  cursor: pointer;
}

.composer {
  display: flex;
  gap: 10px;
  align-items: flex-end;
  padding: 12px 16px 14px;
  border-top: 1px solid var(--color-border);
}
.composer-input {
  flex: 1;
  min-height: 42px;
  max-height: 120px;
  resize: none;
  border-radius: 12px;
  border: 1px solid var(--color-border);
  background: var(--color-surface-2);
  color: var(--color-text);
  padding: 11px 14px;
  font-size: 13.5px;
  font-family: inherit;
  line-height: 1.5;
  outline: none;
  transition: border-color 0.16s;
}
.composer-input:focus {
  border-color: var(--color-brand);
}
.send-btn {
  border-radius: 12px;
  height: 42px;
}

/* ---------- 群聊选择成员 ---------- */
.member-picker {
  max-height: 220px;
  overflow-y: auto;
  width: 100%;
}
.picker-item {
  display: flex;
  margin-right: 0;
  margin-bottom: 4px;
}

/* ---------- 群资料抽屉 ---------- */
.group-detail {
  display: flex;
  flex-direction: column;
  gap: 20px;
}
.gd-head {
  display: flex;
  gap: 13px;
  align-items: center;
}
.gd-name {
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text);
}
.gd-sub {
  font-size: 12px;
  color: var(--color-text-muted);
}
.gd-label {
  font-size: 12px;
  color: var(--color-text-subtle);
  margin-bottom: 6px;
}
.gd-value {
  font-size: 13px;
  color: var(--color-text);
  line-height: 1.6;
}
.gd-members {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.gd-member {
  display: flex;
  align-items: center;
  gap: 9px;
}
.gd-member-name {
  flex: 1;
  font-size: 13px;
  color: var(--color-text);
}
.tag-owner,
.tag-admin {
  font-size: 11px;
  padding: 1px 7px;
  border-radius: 6px;
}
.tag-owner {
  background: var(--color-brand-lighter);
  color: var(--color-brand-dark);
}
.tag-admin {
  background: var(--color-accent-light);
  color: #fff;
}
.gd-actions {
  padding-top: 6px;
}
</style>
