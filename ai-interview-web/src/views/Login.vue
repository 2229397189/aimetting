<template>
  <div class="login-page">
    <div class="login-page__bg"></div>

    <div class="login-box">
      <!-- 左侧品牌介绍 -->
      <section class="login-brand">
        <div class="brand-logo">
          <span class="brand-mark">AI</span>
          <span class="brand-name">模拟面试平台</span>
        </div>
        <h1 class="brand-title">用 AI 还原一场真实的技术面试</h1>
        <ul class="brand-points">
          <li><el-icon><Select /></el-icon>8 大方向 · 3 档难度 · 个性化出题</li>
          <li><el-icon><ChatLineRound /></el-icon>逐题作答，AI 实时评分并深度追问</li>
          <li><el-icon><DataAnalysis /></el-icon>五维能力雷达图 + 逐题改进参考答案</li>
          <li><el-icon><MagicStick /></el-icon>简历解析驱动，越练越有针对性</li>
        </ul>
      </section>

      <!-- 右侧表单 -->
      <section class="login-form">
        <el-tabs v-model="activeTab" class="form-tabs">
          <el-tab-pane label="登录" name="login" />
          <el-tab-pane label="注册" name="register" />
        </el-tabs>

        <!-- 登录表单 -->
        <el-form
          v-if="activeTab === 'login'"
          ref="loginFormRef"
          :model="loginForm"
          :rules="loginRules"
          label-position="top"
          size="large"
          @keyup.enter="submitLogin"
        >
          <el-form-item prop="username" label="用户名 / 邮箱">
            <el-input
              v-model="loginForm.username"
              placeholder="请输入用户名或邮箱"
              :prefix-icon="User"
              clearable
            />
          </el-form-item>
          <el-form-item prop="password" label="密码">
            <el-input
              v-model="loginForm.password"
              type="password"
              placeholder="请输入密码"
              :prefix-icon="Lock"
              show-password
              clearable
            />
          </el-form-item>
          <div class="form-extra">
            <el-checkbox v-model="loginForm.remember">记住我</el-checkbox>
          </div>
          <el-button type="primary" class="submit-btn" :loading="submitting" @click="submitLogin">
            登录
          </el-button>
        </el-form>

        <!-- 注册表单 -->
        <el-form
          v-else
          ref="registerFormRef"
          :model="registerForm"
          :rules="registerRules"
          label-position="top"
          size="large"
          @keyup.enter="submitRegister"
        >
          <el-form-item prop="username" label="用户名">
            <el-input
              v-model="registerForm.username"
              placeholder="4-20 位字母、数字或下划线"
              :prefix-icon="User"
              clearable
            />
          </el-form-item>
          <el-form-item prop="email" label="邮箱">
            <el-input
              v-model="registerForm.email"
              placeholder="请输入邮箱"
              :prefix-icon="Message"
              clearable
            />
          </el-form-item>
          <el-form-item prop="password" label="密码">
            <el-input
              v-model="registerForm.password"
              type="password"
              placeholder="8-20 位，需同时包含字母和数字"
              :prefix-icon="Lock"
              show-password
            />
          </el-form-item>
          <el-form-item prop="confirmPassword" label="确认密码">
            <el-input
              v-model="registerForm.confirmPassword"
              type="password"
              placeholder="请再次输入密码"
              :prefix-icon="Lock"
              show-password
            />
          </el-form-item>
          <el-button type="primary" class="submit-btn" :loading="submitting" @click="submitRegister">
            注册并登录
          </el-button>
        </el-form>

        <p class="form-tip">
          {{ activeTab === 'login' ? '还没有账号？点击上方「注册」立即创建' : '注册成功后将自动为你登录' }}
        </p>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, FormInstance, FormRules } from 'element-plus'
import { Lock, Message, User } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { useConfigStore } from '@/stores/config'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const configStore = useConfigStore()

const activeTab = ref<'login' | 'register'>('login')
const submitting = ref<boolean>(false)

const loginFormRef = ref<FormInstance | null>(null)
const registerFormRef = ref<FormInstance | null>(null)

/** 登录表单 */
const loginForm = reactive({
  username: '',
  password: '',
  remember: true,
})

/** 注册表单 */
const registerForm = reactive({
  username: '',
  email: '',
  password: '',
  confirmPassword: '',
})

/** 用户名校验：4-20 位字母数字下划线，或邮箱 */
function validateAccount(_rule: unknown, value: string, callback: (e?: Error) => void): void {
  if (!value) {
    callback(new Error('请输入用户名或邮箱'))
    return
  }
  const isEmail = value.indexOf('@') > 0
  if (isEmail) {
    const emailReg = /^[\w.+-]+@[\w-]+\.[\w.-]+$/
    if (!emailReg.test(value)) {
      callback(new Error('邮箱格式不正确'))
      return
    }
    callback()
    return
  }
  const nameReg = /^[a-zA-Z0-9_]{4,20}$/
  if (!nameReg.test(value)) {
    callback(new Error('用户名为 4-20 位字母、数字或下划线'))
    return
  }
  callback()
}

/** 密码策略校验：8-20 位且同时含字母与数字（BR-18） */
function validatePassword(_rule: unknown, value: string, callback: (e?: Error) => void): void {
  if (!value) {
    callback(new Error('请输入密码'))
    return
  }
  if (value.length < 8 || value.length > 20) {
    callback(new Error('密码长度需为 8-20 位'))
    return
  }
  if (!/[a-zA-Z]/.test(value) || !/\d/.test(value)) {
    callback(new Error('密码需同时包含字母和数字'))
    return
  }
  callback()
}

const loginRules: FormRules = {
  username: [{ validator: validateAccount, trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

const registerRules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    {
      pattern: /^[a-zA-Z0-9_]{4,20}$/,
      message: '用户名为 4-20 位字母、数字或下划线',
      trigger: 'blur',
    },
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' },
  ],
  password: [{ validator: validatePassword, trigger: 'blur' }],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    {
      validator: (_rule: unknown, value: string, callback: (e?: Error) => void) => {
        if (value !== registerForm.password) {
          callback(new Error('两次输入的密码不一致'))
          return
        }
        callback()
      },
      trigger: 'blur',
    },
  ],
}

/** 登录提交 */
async function submitLogin(): Promise<void> {
  if (!loginFormRef.value) return
  try {
    await loginFormRef.value.validate()
  } catch (e) {
    return
  }
  submitting.value = true
  try {
    await userStore.login({
      username: loginForm.username.trim(),
      password: loginForm.password,
      remember: loginForm.remember,
    })
    ElMessage.success('登录成功，欢迎回来')
    const redirect = (route.query.redirect as string) || '/'
    router.replace(redirect)
  } catch (e: unknown) {
    // 错误已由 request 拦截器统一提示，这里只结束 loading
  } finally {
    submitting.value = false
  }
}

/** 注册提交：成功后自动登录 */
async function submitRegister(): Promise<void> {
  if (!registerFormRef.value) return
  try {
    await registerFormRef.value.validate()
  } catch (e) {
    return
  }
  submitting.value = true
  try {
    await userStore.register({
      username: registerForm.username.trim(),
      email: registerForm.email.trim(),
      password: registerForm.password,
      confirmPassword: registerForm.confirmPassword,
    })
    ElMessage.success('注册成功，正在为你登录…')
    await userStore.login({
      username: registerForm.username.trim(),
      password: registerForm.password,
    })
    router.replace('/')
  } catch (e: unknown) {
    /* 已统一提示 */
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  // 预加载字典与客户端配置，登录后立即可用
  void configStore.ensureLoaded()
})
</script>

<style scoped lang="scss">
.login-page {
  position: relative;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  box-sizing: border-box;
  overflow: hidden;
}

.login-page__bg {
  position: absolute;
  inset: 0;
  background: var(--grad-page);
}

.login-box {
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: 960px;
  display: grid;
  grid-template-columns: 1fr 1fr;
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-xl);
  overflow: hidden;
}

.login-brand {
  padding: 40px 36px;
  background: var(--grad-brand);
  color: #fff;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.brand-logo {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 26px;
}

.brand-mark {
  width: 34px;
  height: 34px;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.22);
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
}

.brand-name {
  font-size: 17px;
  font-weight: 700;
}

.brand-title {
  font-size: 22px;
  line-height: 1.5;
  margin-bottom: 20px;
  color: #fff;
}

.brand-points {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
  font-size: 13px;
  opacity: 0.95;
}

.brand-points li {
  display: flex;
  align-items: center;
  gap: 8px;
}

.login-form {
  padding: 32px 36px 28px;
}

.form-tabs {
  margin-bottom: 8px;
}

.form-extra {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.submit-btn {
  width: 100%;
  height: 42px;
  font-size: 15px;
}

.form-tip {
  margin-top: 14px;
  font-size: 12px;
  color: var(--text-secondary);
  text-align: center;
}

@media (max-width: 900px) {
  .login-box {
    grid-template-columns: 1fr;
    max-width: 460px;
  }

  .login-brand {
    padding: 26px 24px;
  }

  .brand-points {
    display: none;
  }

  .login-form {
    padding: 24px;
  }
}
</style>
