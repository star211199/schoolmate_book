<template>
  <div class="page-container" v-loading="loading">
    <el-card class="card-shadow">
      <template #header>
        <div class="card-title">个人中心</div>
      </template>

      <el-row :gutter="24">
        <el-col :xs="24" :md="7" class="center">
          <el-avatar :size="96" :src="user?.avatar">
            {{ (user?.nickname || 'U').charAt(0) }}
          </el-avatar>
          <div class="upload-tip">
            <el-upload :show-file-list="false" :before-upload="handleAvatarUpload">
              <el-button size="small" plain>更换头像</el-button>
            </el-upload>
          </div>
          <h3>{{ user?.nickname }}</h3>
          <div class="text-muted">@{{ user?.username }}</div>
          <el-tag class="role-tag" :type="user?.role === 'ADMIN' ? 'danger' : 'primary'">
            {{ user?.role === 'ADMIN' ? '管理员' : '普通用户' }}
          </el-tag>
        </el-col>

        <el-col :xs="24" :md="17">
          <el-tabs v-model="tab">
            <el-tab-pane label="基本资料" name="base">
              <el-form :model="baseForm" label-width="90px" style="max-width: 480px">
                <el-form-item label="昵称">
                  <el-input v-model="baseForm.nickname" />
                </el-form-item>
                <el-form-item label="手机号">
                  <el-input v-model="baseForm.phone" placeholder="11 位手机号" />
                </el-form-item>
                <el-form-item label="邮箱">
                  <el-input v-model="baseForm.email" />
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" :loading="saving" @click="saveBase">保存</el-button>
                </el-form-item>
              </el-form>
            </el-tab-pane>

            <el-tab-pane label="详细资料" name="profile">
              <el-form :model="profileForm" label-width="90px" style="max-width: 480px">
                <el-form-item label="真实姓名">
                  <el-input v-model="profileForm.realName" />
                </el-form-item>
                <el-form-item label="学号">
                  <el-input v-model="profileForm.studentNo" />
                </el-form-item>
                <el-form-item label="性别">
                  <el-radio-group v-model="profileForm.gender">
                    <el-radio value="MALE">男</el-radio>
                    <el-radio value="FEMALE">女</el-radio>
                    <el-radio value="UNKNOWN">保密</el-radio>
                  </el-radio-group>
                </el-form-item>
                <el-form-item label="生日">
                  <el-date-picker v-model="profileForm.birthday" type="date" value-format="YYYY-MM-DD" />
                </el-form-item>
                <el-form-item label="籍贯">
                  <el-input v-model="profileForm.hometown" />
                </el-form-item>
                <el-form-item label="现居城市">
                  <el-input v-model="profileForm.currentCity" />
                </el-form-item>
                <el-form-item label="联系方式">
                  <el-input v-model="profileForm.contact" placeholder="微信号 / QQ" />
                </el-form-item>
                <el-form-item label="个性签名">
                  <el-input v-model="profileForm.motto" type="textarea" :rows="2" />
                </el-form-item>
                <el-form-item label="入学年份">
                  <el-input v-model="profileForm.enrollmentYear" />
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" :loading="savingProfile" @click="saveProfile">
                    保存
                  </el-button>
                </el-form-item>
              </el-form>
            </el-tab-pane>

            <el-tab-pane label="修改密码" name="password">
              <el-form :model="pwdForm" label-width="90px" style="max-width: 480px">
                <el-form-item label="原密码">
                  <el-input v-model="pwdForm.oldPassword" type="password" show-password />
                </el-form-item>
                <el-form-item label="新密码">
                  <el-input v-model="pwdForm.newPassword" type="password" show-password />
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" :loading="savingPwd" @click="savePassword">
                    修改密码
                  </el-button>
                </el-form-item>
              </el-form>
            </el-tab-pane>
          </el-tabs>
        </el-col>
      </el-row>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { getUser, updateUser, updatePassword, getProfile, updateProfile, uploadFile } from '@/api/user'
import { ElMessage } from 'element-plus'

const userStore = useUserStore()
const loading = ref(false)
const saving = ref(false)
const savingProfile = ref(false)
const savingPwd = ref(false)
const tab = ref('base')
const user = ref(null)

const baseForm = reactive({ nickname: '', phone: '', email: '' })
const profileForm = reactive({
  realName: '', studentNo: '', gender: 'UNKNOWN', birthday: '',
  hometown: '', currentCity: '', contact: '', motto: '', enrollmentYear: ''
})
const pwdForm = reactive({ oldPassword: '', newPassword: '' })

async function loadAll() {
  loading.value = true
  try {
    const uid = userStore.userId
    const [userRes, profileRes] = await Promise.all([getUser(uid), getProfile(uid)])
    user.value = userRes.data
    baseForm.nickname = userRes.data.nickname || ''
    baseForm.phone = userRes.data.phone || ''
    baseForm.email = userRes.data.email || ''
    Object.assign(profileForm, profileRes.data || {})
  } finally {
    loading.value = false
  }
}

async function saveBase() {
  saving.value = true
  try {
    await updateUser(userStore.userId, {
      nickname: baseForm.nickname,
      phone: baseForm.phone,
      email: baseForm.email
    })
    ElMessage.success('保存成功')
    await userStore.fetchUserInfo()
    loadAll()
  } finally {
    saving.value = false
  }
}

async function saveProfile() {
  savingProfile.value = true
  try {
    await updateProfile(userStore.userId, profileForm)
    ElMessage.success('保存成功')
  } finally {
    savingProfile.value = false
  }
}

async function savePassword() {
  savingPwd.value = true
  try {
    await updatePassword(userStore.userId, pwdForm)
    ElMessage.success('密码修改成功，请重新登录')
    pwdForm.oldPassword = ''
    pwdForm.newPassword = ''
  } finally {
    savingPwd.value = false
  }
}

async function handleAvatarUpload(file) {
  const formData = new FormData()
  formData.append('file', file)
  const res = await uploadFile(formData)
  await updateUser(userStore.userId, { avatar: res.data.url })
  ElMessage.success('头像已更新')
  await userStore.fetchUserInfo()
  loadAll()
  return false
}

onMounted(loadAll)
</script>

<style scoped>
.card-title {
  font-size: 16px;
  font-weight: 600;
}

.center {
  text-align: center;
}

.upload-tip {
  margin: 12px 0;
}

.role-tag {
  margin-top: 8px;
}
</style>
