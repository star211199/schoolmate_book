<template>
  <div class="page-container">
    <el-card class="card-shadow toolbar">
      <div class="toolbar-row">
        <el-input
          v-model="keyword"
          placeholder="搜索班级名称或专业"
          clearable
          style="width: 260px"
          @keyup.enter="loadClasses"
          @clear="loadClasses"
        >
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-button type="primary" @click="loadClasses">搜索</el-button>
        <div class="spacer" />
        <el-button @click="joinVisible = true">
          <el-icon><Promotion /></el-icon> 加入班级
        </el-button>
        <el-button type="primary" @click="createVisible = true">
          <el-icon><Plus /></el-icon> 创建班级
        </el-button>
      </div>
    </el-card>

    <el-row :gutter="16" class="class-list" v-loading="loading">
      <el-col v-for="item in classes" :key="item.id" :xs="24" :sm="12" :md="8">
        <el-card class="class-card card-shadow" shadow="hover" @click="goDetail(item.id)">
          <template #header>
            <div class="card-header">
              <span class="class-name">{{ item.className }}</span>
              <el-tag v-if="item.currentUserRole === 'OWNER'" type="warning" size="small">
                我创建的
              </el-tag>
              <el-tag v-else-if="item.currentUserRole" type="success" size="small">已加入</el-tag>
            </div>
          </template>
          <div class="class-meta">
            <el-tag size="small">{{ item.grade || '未填年级' }}</el-tag>
            <el-tag size="small" type="info">{{ item.major || '未填专业' }}</el-tag>
          </div>
          <p class="desc">{{ item.description || '这个班级还没有简介~' }}</p>
          <div class="card-footer">
            <span class="text-muted">
              <el-icon><User /></el-icon> {{ item.memberCount }} 位成员
            </span>
            <el-text type="primary" size="small">进入班级 →</el-text>
          </div>
        </el-card>
      </el-col>
      <el-col v-if="!loading && classes.length === 0" :span="24">
        <el-empty description="暂无班级，快去创建或加入一个吧" />
      </el-col>
    </el-row>

    <div class="pagination">
      <el-pagination
        v-model:current-page="pageNum"
        :page-size="pageSize"
        :total="total"
        layout="prev, pager, next"
        @current-change="loadClasses"
      />
    </div>

    <!-- 创建班级 -->
    <el-dialog v-model="createVisible" title="创建班级" width="480px">
      <el-form :model="classForm" label-width="80px">
        <el-form-item label="班级名称" required>
          <el-input v-model="classForm.className" placeholder="如：2024级智能科学与技术1班" />
        </el-form-item>
        <el-form-item label="年级">
          <el-input v-model="classForm.grade" placeholder="如：2024级" />
        </el-form-item>
        <el-form-item label="专业">
          <el-input v-model="classForm.major" placeholder="如：智能科学与技术" />
        </el-form-item>
        <el-form-item label="班级简介">
          <el-input v-model="classForm.description" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleCreate">创建</el-button>
      </template>
    </el-dialog>

    <!-- 加入班级 -->
    <el-dialog v-model="joinVisible" title="加入班级" width="400px">
      <el-form label-width="80px">
        <el-form-item label="邀请码" required>
          <el-input v-model="inviteCode" placeholder="向班长索取邀请码" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="joinVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleJoin">加入</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { pageClasses, createClass, joinClass } from '@/api/class'
import { ElMessage } from 'element-plus'

const router = useRouter()
const loading = ref(false)
const submitting = ref(false)
const classes = ref([])
const keyword = ref('')
const pageNum = ref(1)
const pageSize = ref(9)
const total = ref(0)

const createVisible = ref(false)
const joinVisible = ref(false)
const inviteCode = ref('')
const classForm = ref({ className: '', grade: '', major: '', description: '' })

async function loadClasses() {
  loading.value = true
  try {
    const res = await pageClasses({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      keyword: keyword.value || undefined
    })
    classes.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

async function handleCreate() {
  if (!classForm.value.className) {
    ElMessage.warning('请输入班级名称')
    return
  }
  submitting.value = true
  try {
    await createClass(classForm.value)
    ElMessage.success('创建成功')
    createVisible.value = false
    classForm.value = { className: '', grade: '', major: '', description: '' }
    loadClasses()
  } finally {
    submitting.value = false
  }
}

async function handleJoin() {
  if (!inviteCode.value) {
    ElMessage.warning('请输入邀请码')
    return
  }
  submitting.value = true
  try {
    await joinClass({ inviteCode: inviteCode.value })
    ElMessage.success('加入成功')
    joinVisible.value = false
    inviteCode.value = ''
    loadClasses()
  } finally {
    submitting.value = false
  }
}

function goDetail(id) {
  router.push(`/classes/${id}`)
}

onMounted(loadClasses)
</script>

<style scoped>
.toolbar {
  margin-bottom: 16px;
}

.toolbar-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.spacer {
  flex: 1;
}

.class-list {
  margin-top: 4px;
}

.class-card {
  margin-bottom: 16px;
  cursor: pointer;
  transition: transform 0.2s;
}

.class-card:hover {
  transform: translateY(-4px);
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.class-name {
  font-weight: 600;
  font-size: 15px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.class-meta {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
}

.desc {
  color: #606266;
  font-size: 13px;
  line-height: 1.6;
  height: 42px;
  overflow: hidden;
  margin: 8px 0;
}

.card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.pagination {
  display: flex;
  justify-content: center;
  margin-top: 8px;
}
</style>
