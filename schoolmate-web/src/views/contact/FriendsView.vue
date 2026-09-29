<template>
  <div class="contacts-page">
    <div class="glass-card head-card">
      <div class="head-left">
        <h2 class="anime-title">联系人</h2>
        <p class="text-muted">好友、申请与群组都在这里管理</p>
      </div>
      <div class="head-stats">
        <div class="stat">
          <b>{{ friends.length }}</b>
          <span>好友</span>
        </div>
        <div class="stat">
          <b>{{ groups.length }}</b>
          <span>群组</span>
        </div>
        <div class="stat">
          <b :class="{ warn: pendingCount > 0 }">{{ pendingCount }}</b>
          <span>待处理</span>
        </div>
      </div>
    </div>

    <div class="glass-card body-card">
      <el-tabs v-model="tab">
        <!-- ============ 好友 ============ -->
        <el-tab-pane label="好友" name="friends">
          <div v-loading="loadingFriends" class="friend-list">
            <div v-for="f in friends" :key="f.userId" class="friend-item">
              <div class="avatar" :class="{ online: f.online }">
                <img v-if="f.avatar" :src="f.avatar" alt="" />
                <span v-else>{{ (f.displayName || '?').slice(0, 1) }}</span>
              </div>
              <div class="info">
                <div class="name">
                  {{ f.displayName }}
                  <span v-if="f.remark && f.nickname !== f.remark" class="sub">（{{ f.nickname }}）</span>
                </div>
                <div class="meta">
                  <span class="status" :class="{ on: f.online }">{{ f.online ? '在线' : '离线' }}</span>
                  <span v-if="f.motto" class="motto">· {{ f.motto }}</span>
                </div>
              </div>
              <div class="actions">
                <el-button size="small" type="primary" @click="startChat(f)">发消息</el-button>
                <el-dropdown trigger="click" @command="(cmd) => onFriendCommand(cmd, f)">
                  <el-button size="small" text>
                    <el-icon><MoreFilled /></el-icon>
                  </el-button>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item command="remark">设置备注</el-dropdown-item>
                      <el-dropdown-item command="profile">查看主页</el-dropdown-item>
                      <el-dropdown-item command="delete" divided>删除好友</el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
              </div>
            </div>
            <el-empty v-if="!loadingFriends && !friends.length" description="还没有好友" :image-size="90">
              <el-button type="primary" @click="tab = 'search'">去找同学</el-button>
            </el-empty>
          </div>
        </el-tab-pane>

        <!-- ============ 新的朋友 ============ -->
        <el-tab-pane name="requests">
          <template #label>
            <span>新的朋友</span>
            <span v-if="pendingCount" class="tab-badge">{{ pendingCount }}</span>
          </template>

          <div class="req-section" v-loading="loadingRequests">
            <div class="section-title">收到的申请</div>
            <div v-for="r in received" :key="r.id" class="req-item">
              <div class="avatar">
                <img v-if="r.avatar" :src="r.avatar" alt="" />
                <span v-else>{{ (r.nickname || '?').slice(0, 1) }}</span>
              </div>
              <div class="info">
                <div class="name">{{ r.realName || r.nickname }}</div>
                <div class="meta">{{ r.message || '请求添加你为好友' }}</div>
              </div>
              <div class="actions">
                <template v-if="r.status === 'PENDING'">
                  <el-button size="small" type="primary" @click="onAccept(r)">同意</el-button>
                  <el-button size="small" @click="onReject(r)">拒绝</el-button>
                </template>
                <span v-else-if="r.status === 'ACCEPTED'" class="done ok">已同意</span>
                <span v-else class="done">已拒绝</span>
              </div>
            </div>
            <el-empty v-if="!received.length" description="暂无收到的申请" :image-size="70" />

            <div class="section-title" style="margin-top: 22px">我发出的申请</div>
            <div v-for="r in sent" :key="'s' + r.id" class="req-item">
              <div class="avatar">
                <img v-if="r.avatar" :src="r.avatar" alt="" />
                <span v-else>{{ (r.nickname || '?').slice(0, 1) }}</span>
              </div>
              <div class="info">
                <div class="name">{{ r.realName || r.nickname }}</div>
                <div class="meta">{{ r.message }}</div>
              </div>
              <span class="done">{{ statusText(r.status) }}</span>
            </div>
            <el-empty v-if="!sent.length" description="暂无发出的申请" :image-size="70" />
          </div>
        </el-tab-pane>

        <!-- ============ 群组 ============ -->
        <el-tab-pane label="群组" name="groups">
          <div v-loading="loadingGroups" class="friend-list">
            <div v-for="g in groups" :key="g.id" class="friend-item" @click="enterGroup(g)">
              <div class="avatar group">
                <img v-if="g.avatar" :src="g.avatar" alt="" />
                <span v-else>{{ (g.groupName || '?').slice(0, 1) }}</span>
              </div>
              <div class="info">
                <div class="name">
                  {{ g.groupName }}
                  <span v-if="g.groupType === 'CLASS'" class="class-tag">班级群</span>
                </div>
                <div class="meta">
                  <span>{{ g.memberCount }} 位成员</span>
                  <span v-if="g.notice" class="motto">· {{ g.notice }}</span>
                </div>
              </div>
              <div class="actions">
                <el-button size="small" text @click.stop="enterGroup(g)">进入群聊</el-button>
              </div>
            </div>
            <el-empty v-if="!loadingGroups && !groups.length" description="还没有加入任何群组" :image-size="90" />
          </div>
        </el-tab-pane>

        <!-- ============ 找人 ============ -->
        <el-tab-pane label="找人" name="search">
          <div class="search-bar">
            <el-input
              v-model="keyword"
              placeholder="输入账号、昵称或真实姓名搜索同学"
              clearable
              @keyup.enter="onSearch"
            >
              <template #append>
                <el-button @click="onSearch">
                  <el-icon><Search /></el-icon>
                </el-button>
              </template>
            </el-input>
          </div>

          <div v-loading="searching" class="friend-list">
            <div v-for="u in searchResult" :key="u.userId" class="friend-item">
              <div class="avatar">
                <img v-if="u.avatar" :src="u.avatar" alt="" />
                <span v-else>{{ (u.realName || u.nickname || '?').slice(0, 1) }}</span>
              </div>
              <div class="info">
                <div class="name">{{ u.realName || u.nickname }}</div>
                <div class="meta">{{ u.motto || '这位同学还没有留下签名' }}</div>
              </div>
              <div class="actions">
                <el-button v-if="u.relation === 'NONE'" size="small" type="primary" @click="onAddFriend(u)">
                  加好友
                </el-button>
                <el-button v-else-if="u.relation === 'IS_FRIEND'" size="small" @click="startChatWithUser(u)">
                  发消息
                </el-button>
                <el-button
                  v-else-if="u.relation === 'PENDING_RECEIVED'"
                  size="small"
                  type="primary"
                  @click="tab = 'requests'"
                >
                  待你处理
                </el-button>
                <span v-else class="done">等待验证</span>
              </div>
            </div>
            <el-empty
              v-if="!searching && searched && !searchResult.length"
              description="没有找到匹配的同学"
              :image-size="80"
            />
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>

    <!-- 设置备注 -->
    <el-dialog v-model="remarkDialog" title="设置备注" width="380px">
      <el-input v-model="remarkForm.remark" placeholder="给这位同学起个备注名" maxlength="20" />
      <template #footer>
        <el-button @click="remarkDialog = false">取消</el-button>
        <el-button type="primary" @click="submitRemark">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useChatStore } from '@/stores/chat'
import {
  getFriends,
  deleteFriend,
  updateFriend,
  getReceivedRequests,
  getSentRequests,
  acceptRequest,
  rejectRequest,
  searchUsers,
  sendFriendRequest
} from '@/api/friend'
import { getMyGroups } from '@/api/group'

const router = useRouter()
const chatStore = useChatStore()

const tab = ref('friends')
const friends = ref([])
const received = ref([])
const sent = ref([])
const groups = ref([])
const keyword = ref('')
const searchResult = ref([])
const searched = ref(false)

const loadingFriends = ref(false)
const loadingRequests = ref(false)
const loadingGroups = ref(false)
const searching = ref(false)

const remarkDialog = ref(false)
const remarkForm = ref({ userId: null, remark: '' })

const pendingCount = computed(() => received.value.filter((r) => r.status === 'PENDING').length)

function statusText(status) {
  return { PENDING: '等待验证', ACCEPTED: '已同意', REJECTED: '已拒绝', EXPIRED: '已过期' }[status] || status
}

async function loadFriends() {
  loadingFriends.value = true
  try {
    const res = await getFriends()
    friends.value = res.data || []
  } finally {
    loadingFriends.value = false
  }
}

async function loadRequests() {
  loadingRequests.value = true
  try {
    const [r1, r2] = await Promise.all([getReceivedRequests(), getSentRequests()])
    received.value = r1.data || []
    sent.value = r2.data || []
    chatStore.friendRequestCount = pendingCount.value
  } finally {
    loadingRequests.value = false
  }
}

async function loadGroups() {
  loadingGroups.value = true
  try {
    const res = await getMyGroups()
    groups.value = res.data || []
  } finally {
    loadingGroups.value = false
  }
}

async function onAccept(r) {
  await acceptRequest(r.id)
  ElMessage.success('已同意，可以开始聊天了')
  await Promise.all([loadRequests(), loadFriends(), chatStore.loadSessions()])
}

async function onReject(r) {
  await rejectRequest(r.id)
  ElMessage.info('已拒绝该申请')
  await loadRequests()
}

async function onSearch() {
  if (!keyword.value.trim()) {
    ElMessage.warning('请输入搜索关键词')
    return
  }
  searching.value = true
  searched.value = true
  try {
    const res = await searchUsers(keyword.value.trim())
    searchResult.value = res.data || []
  } finally {
    searching.value = false
  }
}

async function onAddFriend(u) {
  await sendFriendRequest({
    toUserId: u.userId,
    message: '你好，我是同校同学，想加你为好友'
  })
  ElMessage.success('好友申请已发送')
  searchResult.value = searchResult.value.map((item) =>
    item.userId === u.userId ? { ...item, relation: 'PENDING_SENT' } : item
  )
  await loadRequests()
}

async function startChat(f) {
  const sessionId = await chatStore.startPrivateChat(f.userId)
  router.push({ path: '/chat', query: { sessionId } })
}

async function startChatWithUser(u) {
  const sessionId = await chatStore.startPrivateChat(u.userId)
  router.push({ path: '/chat', query: { sessionId } })
}

function enterGroup(g) {
  if (!g.sessionId) {
    ElMessage.warning('该群会话尚未就绪，请刷新后重试')
    return
  }
  router.push({ path: '/chat', query: { sessionId: g.sessionId } })
}

function onFriendCommand(cmd, f) {
  if (cmd === 'remark') {
    remarkForm.value = { userId: f.userId, remark: f.remark || '' }
    remarkDialog.value = true
  } else if (cmd === 'profile') {
    router.push(`/users/${f.userId}`)
  } else if (cmd === 'delete') {
    ElMessageBox.confirm(`确定删除好友「${f.displayName}」吗？聊天记录会保留。`, '删除好友', {
      type: 'warning'
    }).then(async () => {
      await deleteFriend(f.userId)
      ElMessage.success('已删除好友')
      await loadFriends()
    })
  }
}

async function submitRemark() {
  await updateFriend(remarkForm.value.userId, { remark: remarkForm.value.remark })
  ElMessage.success('备注已保存')
  remarkDialog.value = false
  await loadFriends()
}

watch(tab, (value) => {
  if (value === 'friends') loadFriends()
  if (value === 'requests') loadRequests()
  if (value === 'groups') loadGroups()
})

onMounted(() => {
  loadFriends()
  loadRequests()
  loadGroups()
})
</script>

<style scoped>
.contacts-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.head-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 16px;
}

.head-left h2 {
  margin: 0 0 4px;
  font-size: 20px;
}
.head-left p {
  margin: 0;
}

.head-stats {
  display: flex;
  gap: 26px;
}
.stat {
  text-align: center;
}
.stat b {
  display: block;
  font-size: 22px;
  color: var(--color-brand);
  line-height: 1.2;
}
.stat b.warn {
  color: var(--color-danger);
}
.stat span {
  font-size: 12px;
  color: var(--color-text-muted);
}

.body-card {
  min-height: 420px;
}

.tab-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 16px;
  height: 16px;
  padding: 0 4px;
  margin-left: 5px;
  border-radius: 999px;
  background: var(--color-danger);
  color: #fff;
  font-size: 10px;
  line-height: 1;
}

.friend-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-height: 200px;
}

.friend-item,
.req-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 11px 12px;
  border-radius: 12px;
  transition: background 0.16s;
}
.friend-item:hover,
.req-item:hover {
  background: var(--color-surface-2);
}
.friend-item {
  cursor: default;
}

.avatar {
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
  overflow: hidden;
}
.avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.avatar.group {
  background: var(--color-accent);
  border-radius: 13px;
}

.info {
  flex: 1;
  min-width: 0;
}
.name {
  font-size: 14px;
  font-weight: 500;
  color: var(--color-text);
}
.name .sub {
  font-size: 12px;
  color: var(--color-text-subtle);
  font-weight: 400;
}
.meta {
  font-size: 12.5px;
  color: var(--color-text-muted);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.status {
  color: var(--color-text-subtle);
}
.status.on {
  color: var(--color-online);
}
.motto {
  color: var(--color-text-subtle);
}

.actions {
  display: flex;
  align-items: center;
  gap: 6px;
  flex: none;
}

.class-tag {
  font-size: 11px;
  padding: 1px 7px;
  border-radius: 6px;
  background: var(--color-brand-lighter);
  color: var(--color-brand-dark);
  margin-left: 6px;
}

.done {
  font-size: 12.5px;
  color: var(--color-text-subtle);
}
.done.ok {
  color: var(--color-online);
}

.section-title {
  font-size: 12.5px;
  color: var(--color-text-subtle);
  margin: 8px 0 6px;
}

.search-bar {
  max-width: 480px;
  margin-bottom: 14px;
}
</style>
