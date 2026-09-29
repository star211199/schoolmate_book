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
        </div>

        <el-divider />
        <div class="demo-tip text-muted">
          演示账号：admin / 123456（管理员）· zhangsan / 123456（普通用户）
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import SakuraFall from '@/components/SakuraFall.vue'

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
