<template>
  <div class="login-page">
    <SakuraFall :density="28" />

    <div class="login-wrapper">
      <!-- 左侧插画 -->
      <div class="illustration">
        <img src="/images/login-illustration.webp" alt="校园时光" />
        <div class="illustration-text">
          <h2>樱花落下的季节</h2>
          <p>把青春写进同学录，把回忆留在樱花树下</p>
        </div>
      </div>

      <!-- 右侧登录表单 -->
      <div class="form-side glass-card">
        <div class="title">
          <h1 class="anime-title">大学同学录</h1>
          <p class="text-muted">记录青春，留住回忆 ✿</p>
        </div>

        <el-form ref="formRef" :model="form" :rules="rules" size="large" @keyup.enter="handleLogin">
          <el-form-item prop="username">
            <el-input v-model="form.username" placeholder="请输入账号" :prefix-icon="User" />
          </el-form-item>
          <el-form-item prop="password">
            <el-input
              v-model="form.password"
              type="password"
              show-password
              placeholder="请输入密码"
              :prefix-icon="Lock"
            />
          </el-form-item>
          <el-button type="primary" class="submit-btn" :loading="loading" @click="handleLogin">
            登 录
          </el-button>
        </el-form>

        <div class="footer">
          <span class="text-muted">还没有账号？</span>
          <el-link type="primary" @click="$router.push('/register')">立即注册</el-link>
          <el-divider direction="vertical" />
          <el-link type="info" @click="openForgot">忘记密码</el-link>
        </div>

        <el-divider />
        <div class="demo-tip text-muted">
          演示账号：admin / 123456（管理员）· zhangsan / 123456（普通用户）
        </div>
      </div>
    </div>

    <!-- 找回密码：两步式（发送验证码 → 重置密码） -->
    <el-dialog v-model="forgotVisible" title="找回密码" width="420px" align-center>
      <el-alert
        v-if="mailEnabled === false"
        type="warning"
        :closable="false"
        show-icon
        title="本站未配置邮件服务"
        description="无法发送邮箱验证码，请联系班级管理员在后台「用户管理」中为你重置密码。"
      />
      <el-form
        v-else
        ref="forgotRef"
        :model="forgotForm"
        :rules="forgotRules"
        label-width="76px"
        class="forgot-form"
      >
        <el-form-item label="账号" prop="username">
          <el-input v-model="forgotForm.username" placeholder="你的登录账号" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="forgotForm.email" placeholder="注册时绑定的邮箱" />
        </el-form-item>
        <el-form-item label="验证码" prop="code">
          <div class="code-row">
            <el-input v-model="forgotForm.code" maxlength="6" placeholder="6 位数字" />
            <el-button
              :disabled="countdown > 0"
              :loading="sending"
              @click="handleSendCode"
            >
              {{ countdown > 0 ? `${countdown}s` : '获取验证码' }}
            </el-button>
          </div>
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input
            v-model="forgotForm.newPassword"
            type="password"
            show-password
            placeholder="6-32 位"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="forgotVisible = false">取 消</el-button>
        <el-button
          v-if="mailEnabled !== false"
          type="primary"
          :loading="resetting"
          @click="handleReset"
        >
          重置密码
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onBeforeUnmount } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import SakuraFall from '@/components/SakuraFall.vue'
import { getMailResetEnabled, forgotPassword, resetPassword } from '@/api/auth'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const formRef = ref()
const loading = ref(false)
const form = reactive({ username: 'admin', password: '123456' })

const rules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function handleLogin() {
  await formRef.value.validate()
  loading.value = true
  try {
    await userStore.login(form)
    ElMessage.success('登录成功')
    router.push(route.query.redirect || '/classes')
  } catch (e) {
    // 错误提示由 axios 拦截器统一处理
  } finally {
    loading.value = false
  }
}

/* ==================== 找回密码 ==================== */

const forgotVisible = ref(false)
const forgotRef = ref()
const sending = ref(false)
const resetting = ref(false)
const countdown = ref(0)
/** null=未知（不展示提示）/ true=可用 / false=未配置邮件服务 */
const mailEnabled = ref(null)
let timer = null

const forgotForm = reactive({ username: '', email: '', code: '', newPassword: '' })

const forgotRules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }
  ],
  code: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { len: 6, message: '验证码为 6 位数字', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 32, message: '密码长度需为 6-32 位', trigger: 'blur' }
  ]
}

function openForgot() {
  // 带出已填的账号，少打一次字
  forgotForm.username = form.username
  forgotVisible.value = true
}

/** 校验账号+邮箱这部分单独跑，避免「验证码/新密码」为空也一起报错 */
async function validateFields(fields) {
  try {
    await forgotRef.value.validateField(fields)
    return true
  } catch {
    return false
  }
}

async function handleSendCode() {
  if (!(await validateFields(['username', 'email']))) return
  sending.value = true
  try {
    await forgotPassword({ username: forgotForm.username, email: forgotForm.email })
    ElMessage.success('验证码已发送，请查收邮箱（10 分钟内有效）')
    startCountdown()
  } catch (e) {
    // 拦截器已提示
  } finally {
    sending.value = false
  }
}

function startCountdown() {
  countdown.value = 60
  clearInterval(timer)
  timer = setInterval(() => {
    countdown.value -= 1
    if (countdown.value <= 0) clearInterval(timer)
  }, 1000)
}

async function handleReset() {
  await forgotRef.value.validate()
  resetting.value = true
  try {
    await resetPassword({ ...forgotForm })
    ElMessage.success('密码已重置，请用新密码登录')
    // 顺手把账号和新密码填回登录框
    form.username = forgotForm.username
    form.password = forgotForm.newPassword
    forgotVisible.value = false
  } catch (e) {
    // 拦截器已提示
  } finally {
    resetting.value = false
  }
}

onMounted(async () => {
  try {
    const res = await getMailResetEnabled()
    mailEnabled.value = res.data === true
  } catch {
    mailEnabled.value = null
  }
})

onBeforeUnmount(() => clearInterval(timer))
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}

.login-wrapper {
  position: relative;
  z-index: 2;
  display: flex;
  width: 860px;
  max-width: 95vw;
  min-height: 540px;
  border-radius: 24px;
  overflow: hidden;
  box-shadow: 0 20px 60px rgba(251, 111, 146, 0.2);
}

.illustration {
  position: relative;
  flex: 1;
  min-width: 0;
}

.illustration img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.illustration-text {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 28px;
  background: linear-gradient(transparent, rgba(74, 78, 105, 0.55));
  color: #fff;
}

.illustration-text h2 {
  margin: 0 0 6px;
  font-size: 22px;
  letter-spacing: 2px;
}

.illustration-text p {
  margin: 0;
  font-size: 13px;
  opacity: 0.9;
}

.form-side {
  width: 380px;
  border-radius: 0;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.title {
  text-align: center;
  margin-bottom: 28px;
}

.title h1 {
  margin: 0 0 8px;
  font-size: 28px;
}

.submit-btn {
  width: 100%;
  font-weight: 600;
  letter-spacing: 4px;
}

.footer {
  margin-top: 18px;
  text-align: center;
}

.demo-tip {
  text-align: center;
  line-height: 1.6;
}

.forgot-form {
  margin-top: 8px;
}

.code-row {
  display: flex;
  gap: 10px;
  width: 100%;
}

.code-row .el-button {
  flex: none;
  min-width: 108px;
}

/* 小屏隐藏插画 */
@media (max-width: 720px) {
  .illustration {
    display: none;
  }
  .form-side {
    width: 100%;
    border-radius: 24px;
  }
}
</style>
