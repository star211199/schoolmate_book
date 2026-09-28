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
              <el-form :model="profileForm" label-width="90px" style="max-width: 520px">
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
                  <span class="text-muted" style="margin-left: 8px">保存后自动计算星座</span>
                </el-form-item>
                <el-form-item label="MBTI">
                  <el-select v-model="profileForm.mbti" clearable placeholder="选择你的人格类型" style="width: 200px">
                    <el-option v-for="m in mbtiOptions" :key="m" :label="m" :value="m" />
                  </el-select>
                </el-form-item>
                <el-form-item label="兴趣爱好">
                  <el-select
                    v-model="profileForm.hobbies"
                    multiple
                    filterable
                    allow-create
                    default-first-option
                    placeholder="输入后回车创建标签"
                  />
                </el-form-item>
                <el-form-item label="技能标签">
                  <el-select
                    v-model="profileForm.skills"
                    multiple
                    filterable
                    allow-create
                    default-first-option
                    placeholder="如 Java、摄影、剪辑"
                  />
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
                <el-form-item label="毕业寄语">
                  <el-input
                    v-model="profileForm.graduationMessage"
                    type="textarea"
                    :rows="3"
                    placeholder="写给同学们的话，会展示在你的个人主页"
                  />
                </el-form-item>
                <el-form-item label="社交链接">
                  <div class="social-inputs">
                    <el-input v-model="profileForm.socialQq" placeholder="QQ 号">
                      <template #prepend>QQ</template>
                    </el-input>
                    <el-input v-model="profileForm.socialWechat" placeholder="微信号">
                      <template #prepend>微信</template>
                    </el-input>
                    <el-input v-model="profileForm.socialGithub" placeholder="GitHub 用户名">
                      <template #prepend>GitHub</template>
                    </el-input>
                  </div>
                </el-form-item>
                <el-form-item label="主页封面">
                  <div class="cover-options">
                    <div
                      v-for="c in coverOptions"
                      :key="c"
                      class="cover-option"
                      :class="{ active: profileForm.coverImage === c }"
                      @click="profileForm.coverImage = c"
                    >
                      <img :src="c" alt="封面" />
                    </div>
                  </div>
                </el-form-item>
                <el-form-item label="入学年份">
                  <el-input v-model="profileForm.enrollmentYear" />
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" :loading="savingProfile" @click="saveProfile">
                    保存
                  </el-button>
                  <el-button @click="$router.push(`/user/${userStore.userId}`)">预览我的主页</el-button>
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
  mbti: '', hobbies: [], skills: [], graduationMessage: '',
  socialQq: '', socialWechat: '', socialGithub: '', coverImage: '',
  hometown: '', currentCity: '', contact: '', motto: '', enrollmentYear: ''
})
const pwdForm = reactive({ oldPassword: '', newPassword: '' })

const mbtiOptions = [
  'INTJ', 'INTP', 'ENTJ', 'ENTP', 'INFJ', 'INFP', 'ENFJ', 'ENFP',
  'ISTJ', 'ISFJ', 'ESTJ', 'ESFJ', 'ISTP', 'ISFP', 'ESTP', 'ESFP'
]

const coverOptions = ['/images/banner-graduation.png', '/images/bg-campus.png', '/images/album-sample.png']

async function loadAll() {
  loading.value = true
  try {
    const uid = userStore.userId
    const [userRes, profileRes] = await Promise.all([getUser(uid), getProfile(uid)])
    user.value = userRes.data
    baseForm.nickname = userRes.data.nickname || ''
    baseForm.phone = userRes.data.phone || ''
    baseForm.email = userRes.data.email || ''
    const p = profileRes.data || {}
    Object.assign(profileForm, {
      realName: p.realName || '', studentNo: p.studentNo || '',
      gender: p.gender || 'UNKNOWN', birthday: p.birthday || '',
      mbti: p.mbti || '', hobbies: p.hobbies || [], skills: p.skills || [],
      graduationMessage: p.graduationMessage || '', coverImage: p.coverImage || '',
      hometown: p.hometown || '', currentCity: p.currentCity || '',
      contact: p.contact || '', motto: p.motto || '', enrollmentYear: p.enrollmentYear || '',
      socialQq: p.socialLinks?.qq || '', socialWechat: p.socialLinks?.wechat || '',
      socialGithub: p.socialLinks?.github || ''
    })
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
    const socialLinks = {}
    if (profileForm.socialQq) socialLinks.qq = profileForm.socialQq
    if (profileForm.socialWechat) socialLinks.wechat = profileForm.socialWechat
    if (profileForm.socialGithub) socialLinks.github = profileForm.socialGithub
    await updateProfile(userStore.userId, {
      realName: profileForm.realName,
      studentNo: profileForm.studentNo,
      gender: profileForm.gender,
      birthday: profileForm.birthday || null,
      mbti: profileForm.mbti,
      hobbies: profileForm.hobbies,
      skills: profileForm.skills,
      graduationMessage: profileForm.graduationMessage,
      socialLinks,
      coverImage: profileForm.coverImage,
      hometown: profileForm.hometown,
      currentCity: profileForm.currentCity,
      contact: profileForm.contact,
      motto: profileForm.motto,
      enrollmentYear: profileForm.enrollmentYear
    })
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

.social-inputs {
  display: flex;
  flex-direction: column;
  gap: 10px;
  width: 100%;
}

.cover-options {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.cover-option {
  width: 130px;
  height: 74px;
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
  border: 3px solid transparent;
  transition: all 0.2s;
}

.cover-option.active {
  border-color: var(--el-color-primary);
  box-shadow: 0 4px 12px rgba(251, 111, 146, 0.3);
}

.cover-option img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}
</style>
