<template>
  <div class="page-container capsule-page">
    <!-- 头部 -->
    <div class="glass-card head-card">
      <div class="head-left">
        <h2 class="anime-title">时光胶囊</h2>
        <p class="text-muted">写给未来的自己，或写给全班 —— 到开启那天，信件才会被打开</p>
      </div>
      <el-button type="primary" @click="openWrite">
        <el-icon><EditPen /></el-icon>
        写一封信
      </el-button>
    </div>

    <!-- 我的胶囊 -->
    <div class="glass-card body-card">
      <div class="card-head">
        <h3>我的信箱</h3>
        <span class="text-muted">共 {{ mine.length }} 封，其中 {{ openedCount }} 封已开启</span>
      </div>

      <div v-loading="loadingMine" class="capsule-grid">
        <div
          v-for="c in mine"
          :key="c.id"
          class="capsule-card"
          :class="{ opened: c.openable }"
          @click="openDetail(c)"
        >
          <div class="seal">
            <el-icon><component :is="c.openable ? 'Unlock' : 'Lock'" /></el-icon>
          </div>
          <div class="capsule-title">{{ c.title }}</div>
          <div v-if="c.openable" class="capsule-preview">{{ c.content }}</div>
          <div v-else class="capsule-countdown">
            <span class="num">{{ splitCountdown(c.countdownSeconds).text }}</span>
            <span class="unit">{{ splitCountdown(c.countdownSeconds).unit }}</span>
          </div>
          <div class="capsule-foot">
            <span class="tag">{{ c.openType === 'SELF' ? '写给自己' : '写给全班' }}</span>
            <span class="text-muted">开启于 {{ (c.openTime || '').slice(0, 10) }}</span>
          </div>
          <el-button
            v-if="!c.openable"
            class="del"
            text
            size="small"
            @click.stop="handleDelete(c)"
          >
            撤回
          </el-button>
        </div>

        <el-empty
          v-if="!loadingMine && !mine.length"
          description="还没有写给未来的信"
          :image-size="90"
        >
          <el-button type="primary" @click="openWrite">写第一封</el-button>
        </el-empty>
      </div>
    </div>

    <!-- 班级胶囊墙 -->
    <div class="glass-card body-card">
      <div class="card-head">
        <h3>班级胶囊墙</h3>
        <el-select
          v-model="activeClassId"
          placeholder="选择班级"
          size="small"
          style="width: 200px"
          @change="loadClassCapsules"
        >
          <el-option
            v-for="cl in classes"
            :key="cl.id"
            :label="cl.className"
            :value="cl.id"
          />
        </el-select>
      </div>

      <div v-loading="loadingClass" class="capsule-grid">
        <div
          v-for="c in classCapsules"
          :key="c.id"
          class="capsule-card"
          :class="{ opened: c.openable }"
          @click="openDetail(c)"
        >
          <div class="seal">
            <el-icon><component :is="c.openable ? 'Unlock' : 'Lock'" /></el-icon>
          </div>
          <div class="capsule-title">{{ c.title }}</div>
          <div class="capsule-author">
            <img v-if="c.avatar" :src="c.avatar" alt="" />
            <span v-else class="mini">{{ (c.nickname || '?').slice(0, 1) }}</span>
            <span class="name">{{ c.nickname }}</span>
          </div>
          <div v-if="c.openable" class="capsule-preview">{{ c.content }}</div>
          <div v-else class="capsule-countdown">
            <span class="num">{{ splitCountdown(c.countdownSeconds).text }}</span>
            <span class="unit">{{ splitCountdown(c.countdownSeconds).unit }}</span>
          </div>
          <div class="capsule-foot">
            <span class="text-muted">开启于 {{ (c.openTime || '').slice(0, 10) }}</span>
          </div>
        </div>

        <el-empty
          v-if="!loadingClass && !classCapsules.length"
          :description="classes.length ? '这个班还没有公开的信' : '你还没有加入班级'"
          :image-size="90"
        />
      </div>
    </div>

    <!-- 写信弹窗 -->
    <el-dialog v-model="writeVisible" title="写一封信" width="520px" align-center>
      <el-form ref="writeRef" :model="writeForm" :rules="writeRules" label-width="86px">
        <el-form-item label="写给谁" prop="openType">
          <el-radio-group v-model="writeForm.openType">
            <el-radio-button value="SELF">未来的自己</el-radio-button>
            <el-radio-button value="PUBLIC">全班同学</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item
          v-if="writeForm.openType === 'PUBLIC'"
          label="选择班级"
          prop="classId"
        >
          <el-select v-model="writeForm.classId" placeholder="请选择班级" style="width: 100%">
            <el-option
              v-for="cl in classes"
              :key="cl.id"
              :label="cl.className"
              :value="cl.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="标题" prop="title">
          <el-input v-model="writeForm.title" maxlength="100" show-word-limit placeholder="例如：写给毕业那天的自己" />
        </el-form-item>
        <el-form-item label="内容" prop="content">
          <el-input
            v-model="writeForm.content"
            type="textarea"
            :rows="6"
            maxlength="2000"
            show-word-limit
            placeholder="想对未来说些什么？"
          />
        </el-form-item>
        <el-form-item label="开启时间" prop="openTime">
          <el-date-picker
            v-model="writeForm.openTime"
            type="datetime"
            placeholder="必须是将来的时间"
            value-format="YYYY-MM-DD HH:mm:ss"
            :disabled-date="disablePast"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="writeVisible = false">取 消</el-button>
        <el-button type="primary" :loading="writing" @click="handleWrite">封 存</el-button>
      </template>
    </el-dialog>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" :title="detail?.title || '信件'" width="520px" align-center>
      <div v-if="detail" class="detail-body">
        <div class="detail-meta text-muted">
          {{ detail.nickname }} ·
          {{ detail.openType === 'SELF' ? '写给自己' : '写给全班' }} ·
          开启于 {{ (detail.openTime || '').slice(0, 10) }}
        </div>
        <div v-if="detail.openable" class="detail-content">{{ detail.content }}</div>
        <div v-else class="detail-locked">
          <el-icon class="big"><Lock /></el-icon>
          <p>信件还在封存中</p>
          <p class="text-muted">
            距开启还有 {{ splitCountdown(detail.countdownSeconds).text }}
            {{ splitCountdown(detail.countdownSeconds).unit }}
          </p>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { EditPen, Lock, Unlock } from '@element-plus/icons-vue'
import { myClasses } from '@/api/class'
import {
  createCapsule,
  getMyCapsules,
  getClassCapsules,
  deleteCapsule
} from '@/api/capsule'

const mine = ref([])
const classCapsules = ref([])
const classes = ref([])
const activeClassId = ref(null)

const loadingMine = ref(false)
const loadingClass = ref(false)
const writing = ref(false)

const writeVisible = ref(false)
const detailVisible = ref(false)
const writeRef = ref()
const detail = ref(null)

const openedCount = computed(() => mine.value.filter((c) => c.openable).length)

const writeForm = reactive({
  openType: 'SELF',
  classId: null,
  title: '',
  content: '',
  openTime: ''
})

const writeRules = {
  openType: [{ required: true, message: '请选择收信人', trigger: 'change' }],
  classId: [
    {
      validator: (rule, value, cb) => {
        if (writeForm.openType === 'PUBLIC' && !value) {
          cb(new Error('写给全班需要选择班级'))
        } else {
          cb()
        }
      },
      trigger: 'change'
    }
  ],
  title: [{ required: true, message: '请填写标题', trigger: 'blur' }],
  content: [{ required: true, message: '请写下信件内容', trigger: 'blur' }],
  openTime: [{ required: true, message: '请选择开启时间', trigger: 'change' }]
}

/** 不允许选今天及以前 */
function disablePast(date) {
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  return date.getTime() < today.getTime()
}

/** 倒计时秒 → { text, unit }，单位自动降到最合适的粒度 */
function splitCountdown(seconds) {
  const s = Number(seconds || 0)
  if (s <= 0) return { text: '0', unit: '秒' }
  if (s < 60) return { text: String(s), unit: '秒后开启' }
  if (s < 3600) return { text: String(Math.floor(s / 60)), unit: '分钟后开启' }
  if (s < 86400) return { text: String(Math.floor(s / 3600)), unit: '小时后开启' }
  return { text: String(Math.floor(s / 86400)), unit: '天后开启' }
}

async function loadMine() {
  loadingMine.value = true
  try {
    const res = await getMyCapsules()
    mine.value = res.data || []
  } catch {
    // 拦截器已提示
  } finally {
    loadingMine.value = false
  }
}

async function loadClasses() {
  try {
    const res = await myClasses()
    classes.value = res.data || []
    if (classes.value.length && !activeClassId.value) {
      activeClassId.value = classes.value[0].id
      loadClassCapsules()
    }
  } catch {
    // 拦截器已提示
  }
}

async function loadClassCapsules() {
  if (!activeClassId.value) return
  loadingClass.value = true
  try {
    const res = await getClassCapsules(activeClassId.value)
    classCapsules.value = res.data || []
  } catch {
    // 拦截器已提示
  } finally {
    loadingClass.value = false
  }
}

function openWrite() {
  writeForm.openType = 'SELF'
  writeForm.classId = activeClassId.value || (classes.value[0]?.id ?? null)
  writeForm.title = ''
  writeForm.content = ''
  writeForm.openTime = ''
  writeVisible.value = true
}

async function handleWrite() {
  await writeRef.value.validate()
  writing.value = true
  try {
    await createCapsule({
      title: writeForm.title,
      content: writeForm.content,
      openTime: writeForm.openTime,
      openType: writeForm.openType,
      classId: writeForm.openType === 'PUBLIC' ? writeForm.classId : undefined
    })
    ElMessage.success('信件已封存，静待开启')
    writeVisible.value = false
    loadMine()
    if (writeForm.openType === 'PUBLIC') {
      loadClassCapsules()
    }
  } catch {
    // 拦截器已提示
  } finally {
    writing.value = false
  }
}

async function openDetail(c) {
  // 列表接口已带回内容（未到点时为 null），直接用，避免多余请求
  detail.value = c
  detailVisible.value = true
}

async function handleDelete(c) {
  try {
    await ElMessageBox.confirm(
      '撤回后这封信将被删除，无法找回。确定撤回吗？',
      '撤回信件',
      { type: 'warning', confirmButtonText: '确定撤回', cancelButtonText: '再想想' }
    )
  } catch {
    return
  }
  try {
    await deleteCapsule(c.id)
    ElMessage.success('已撤回')
    loadMine()
  } catch {
    // 拦截器已提示
  }
}

onMounted(() => {
  loadMine()
  loadClasses()
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

.body-card {
  margin-bottom: 16px;
}

.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
}

.card-head h3 {
  margin: 0;
  font-size: 16px;
  color: var(--color-text);
}

.capsule-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 14px;
  min-height: 140px;
}

.capsule-card {
  position: relative;
  padding: 18px 16px 14px;
  border-radius: 16px;
  border: 1px dashed var(--color-brand-light);
  background: var(--color-glass);
  cursor: pointer;
  transition: transform 0.18s ease, box-shadow 0.18s ease;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.capsule-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 10px 26px var(--color-shadow);
}

.capsule-card.opened {
  border-style: solid;
  border-color: var(--color-brand-dark);
  background: var(--color-brand-lighter);
}

.seal {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--color-brand-light);
  color: #fff;
}

.capsule-card.opened .seal {
  background: var(--color-brand-dark);
}

.capsule-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.capsule-preview {
  font-size: 13px;
  color: var(--color-text-muted);
  line-height: 1.6;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
}

.capsule-countdown {
  display: flex;
  align-items: baseline;
  gap: 4px;
  color: var(--color-brand-dark);
}

.capsule-countdown .num {
  font-size: 26px;
  font-weight: 700;
  line-height: 1;
}

.capsule-countdown .unit {
  font-size: 12px;
  color: var(--color-text-muted);
}

.capsule-author {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--color-text-muted);
}

.capsule-author img,
.capsule-author .mini {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  object-fit: cover;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--color-brand-light);
  color: #fff;
  font-size: 11px;
}

.capsule-foot {
  margin-top: auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  font-size: 12px;
}

.capsule-foot .tag {
  padding: 1px 8px;
  border-radius: 999px;
  background: var(--color-brand-lighter);
  color: var(--color-brand-dark);
}

.del {
  position: absolute;
  top: 10px;
  right: 10px;
  opacity: 0;
  transition: opacity 0.18s ease;
}

.capsule-card:hover .del {
  opacity: 1;
}

.detail-body {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.detail-meta {
  font-size: 12px;
}

.detail-content {
  white-space: pre-wrap;
  line-height: 1.9;
  color: var(--color-text);
  max-height: 46vh;
  overflow: auto;
}

.detail-locked {
  text-align: center;
  padding: 28px 0;
  color: var(--color-text);
}

.detail-locked .big {
  font-size: 38px;
  color: var(--color-brand-light);
}

.detail-locked p {
  margin: 8px 0 0;
}
</style>
