<template>
  <div>
    <div class="toolbar">
      <el-select v-model="type" placeholder="内容类型" clearable style="width: 160px" @change="loadAudits">
        <el-option label="全部" value="" />
        <el-option label="留言" value="MESSAGE" />
        <el-option label="动态" value="MOMENT" />
        <el-option label="照片" value="PHOTO" />
      </el-select>
      <el-button type="primary" @click="loadAudits">刷新</el-button>
      <span class="text-muted">
        当前审核开关：{{ auditEnabled ? '开启（内容需审核）' : '关闭（发布即通过）' }}
      </span>
    </div>

    <el-table :data="audits" v-loading="loading" border stripe>
      <el-table-column label="类型" width="90">
        <template #default="{ row }">
          <el-tag size="small">{{ typeText(row.type) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="发布人" width="140">
        <template #default="{ row }">{{ row.nickname || row.userId }}</template>
      </el-table-column>
      <el-table-column label="内容" min-width="320">
        <template #default="{ row }">
          <el-image
            v-if="row.type === 'PHOTO'"
            :src="row.url"
            :preview-src-list="[row.url]"
            fit="cover"
            class="audit-img"
          />
          <span v-else>{{ row.content }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="提交时间" width="170">
        <template #default="{ row }">
          {{ (row.createTime || '').replace('T', ' ').slice(0, 16) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <el-button type="success" link size="small" @click="handleAudit(row, true)">通过</el-button>
          <el-button type="danger" link size="small" @click="handleAudit(row, false)">驳回</el-button>
        </template>
      </el-table-column>
      <template #empty>
        <el-empty description="没有待审核的内容" />
      </template>
    </el-table>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { listPendingAudits, auditContent } from '@/api/admin'
import { ElMessage } from 'element-plus'

const loading = ref(false)
const audits = ref([])
const type = ref('')
// 与后端 schoolmate.content.audit-enabled 对应，仅用于前端提示文案
const auditEnabled = ref(false)

function typeText(t) {
  return { MESSAGE: '留言', MOMENT: '动态', PHOTO: '照片' }[t] || t
}

async function loadAudits() {
  loading.value = true
  try {
    const res = await listPendingAudits(type.value || undefined)
    audits.value = res.data
  } finally {
    loading.value = false
  }
}

async function handleAudit(row, passed) {
  await auditContent(row.type, row.id, passed)
  ElMessage.success(passed ? '已通过' : '已驳回')
  loadAudits()
}

onMounted(loadAudits)
</script>

<style scoped>
.toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.audit-img {
  width: 80px;
  height: 60px;
  border-radius: 4px;
}
</style>
