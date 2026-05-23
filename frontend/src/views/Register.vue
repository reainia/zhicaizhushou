<template>
  <div class="register-page">
    <div class="register-card">
      <div class="register-header">
        <div class="logo">
          <div class="logo-icon">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M12 2v20M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6" />
            </svg>
          </div>
          <span class="logo-text">智财助手</span>
        </div>
        <h1 class="register-title">创建账户</h1>
        <p class="register-subtitle">注册后开始管理您的财务</p>
      </div>

      <a-alert
        v-if="errorMsg"
        :message="errorMsg"
        type="error"
        show-icon
        closable
        class="register-error"
        @close="errorMsg = ''"
      />

      <a-form
        :model="formState"
        :rules="rules"
        layout="vertical"
        @finish="handleSubmit"
        class="register-form"
      >
        <a-form-item name="username" label="用户名">
          <a-input
            v-model:value="formState.username"
            placeholder="请输入用户名"
            size="large"
            autocomplete="username"
          >
            <template #prefix>
              <UserOutlined class="input-prefix" />
            </template>
          </a-input>
        </a-form-item>

        <a-form-item name="email" label="邮箱">
          <a-input
            v-model:value="formState.email"
            placeholder="请输入邮箱地址"
            size="large"
            autocomplete="email"
          >
            <template #prefix>
              <MailOutlined class="input-prefix" />
            </template>
          </a-input>
        </a-form-item>

        <a-form-item name="password" label="密码">
          <a-input-password
            v-model:value="formState.password"
            placeholder="请输入密码"
            size="large"
            autocomplete="new-password"
          >
            <template #prefix>
              <LockOutlined class="input-prefix" />
            </template>
          </a-input-password>
        </a-form-item>

        <a-form-item name="confirmPassword" label="确认密码">
          <a-input-password
            v-model:value="formState.confirmPassword"
            placeholder="请再次输入密码"
            size="large"
            autocomplete="new-password"
          >
            <template #prefix>
              <LockOutlined class="input-prefix" />
            </template>
          </a-input-password>
        </a-form-item>

        <a-form-item class="form-submit">
          <a-button
            type="primary"
            html-type="submit"
            :loading="loading"
            block
            size="large"
            class="register-btn"
          >
            注册
          </a-button>
        </a-form-item>
      </a-form>

      <div class="register-footer">
        <span class="footer-text">已有账户？</span>
        <router-link to="/login" class="login-link">返回登录</router-link>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { UserOutlined, LockOutlined, MailOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import { register } from '../api/auth'
import { useUserStore } from '../stores/user'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const errorMsg = ref('')

interface FormState {
  username: string
  email: string
  password: string
  confirmPassword: string
}

const formState = reactive<FormState>({
  username: '',
  email: '',
  password: '',
  confirmPassword: ''
})

const validateConfirm = (_rule: any, value: string) => {
  if (!value) {
    return Promise.reject(new Error('请再次输入密码'))
  }
  if (value !== formState.password) {
    return Promise.reject(new Error('两次输入的密码不一致'))
  }
  return Promise.resolve()
}

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 2, message: '用户名至少 2 个字符', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱地址', trigger: 'blur' },
    { type: 'email' as const, message: '请输入有效的邮箱地址', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少 6 个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    { validator: validateConfirm, trigger: 'blur' }
  ]
}

async function handleSubmit() {
  loading.value = true
  errorMsg.value = ''

  try {
    const res = await register(formState.username, formState.email, formState.password)
    if (res.token) {
      userStore.setToken(res.token)
      if (res.user) {
        userStore.setUser(res.user)
      }
      message.success('注册成功！')
      router.push('/dashboard')
    } else {
      message.success('注册成功！请登录')
      router.push('/login')
    }
  } catch (error: any) {
    const msg = error?.response?.data?.message
      || error?.response?.data
      || error?.message
      || '注册失败，请稍后重试'
    errorMsg.value = typeof msg === 'string' ? msg : '注册失败，请稍后重试'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.register-page {
  min-height: 100dvh;
  display: flex;
  align-items: center;
  justify-content: center;
  background:
    radial-gradient(ellipse at 20% 50%, rgba(16, 185, 129, 0.06) 0%, transparent 50%),
    radial-gradient(ellipse at 80% 50%, rgba(16, 185, 129, 0.04) 0%, transparent 50%),
    #f8fafc;
  padding: 24px;
}

.register-card {
  width: 100%;
  max-width: 400px;
  background: #ffffff;
  border-radius: 16px;
  padding: 40px 32px 32px;
  box-shadow:
    0 1px 3px rgba(0, 0, 0, 0.04),
    0 8px 24px rgba(0, 0, 0, 0.06);
}

.register-header {
  margin-bottom: 28px;
}

.logo {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 24px;
}

.logo-icon {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  background: linear-gradient(135deg, #10b981, #059669);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
}

.logo-text {
  font-size: 18px;
  font-weight: 700;
  color: #0f172a;
  letter-spacing: -0.01em;
}

.register-title {
  font-size: 24px;
  font-weight: 700;
  color: #0f172a;
  margin: 0 0 6px;
  letter-spacing: -0.02em;
}

.register-subtitle {
  font-size: 14px;
  color: #94a3b8;
  margin: 0;
}

/* Error alert */
.register-error {
  margin-bottom: 20px;
  border-radius: 8px;
}

/* Form overrides */
.register-form :deep(.ant-form-item) {
  margin-bottom: 20px;
}

.register-form :deep(.ant-form-item-label) {
  padding-bottom: 6px;
}

.register-form :deep(.ant-form-item-label label) {
  font-size: 14px;
  font-weight: 500;
  color: #334155;
  height: auto;
}

.register-form :deep(.ant-input-affix-wrapper),
.register-form :deep(.ant-input) {
  border-radius: 10px;
  border: 1.5px solid #e2e8f0;
  padding: 8px 14px;
  font-size: 14px;
  transition: all 0.2s ease;
  background: #fff;
  color: #0f172a;
}

.register-form :deep(.ant-input-affix-wrapper:hover),
.register-form :deep(.ant-input:hover) {
  border-color: #94a3b8;
}

.register-form :deep(.ant-input-affix-wrapper-focused),
.register-form :deep(.ant-input-affix-wrapper:focus) {
  border-color: #10b981;
  box-shadow: 0 0 0 3px rgba(16, 185, 129, 0.1);
}

.register-form :deep(.ant-input-password-icon) {
  color: #94a3b8;
}

.register-form :deep(.ant-input-password-icon:hover) {
  color: #10b981;
}

.register-form :deep(.ant-form-item-explain-error) {
  font-size: 12px;
  margin-top: 4px;
}

.register-form :deep(.ant-input-affix-wrapper > .ant-input) {
  padding: 0;
  border: none;
  box-shadow: none;
}

.input-prefix {
  color: #94a3b8;
  font-size: 14px;
}

.form-submit {
  margin-bottom: 0 !important;
}

.register-btn {
  height: 46px;
  font-size: 15px;
  font-weight: 600;
  border-radius: 10px;
  border: none;
  background: linear-gradient(135deg, #10b981, #059669);
  box-shadow: 0 4px 12px rgba(16, 185, 129, 0.3);
  transition: all 0.2s ease;
}

.register-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 16px rgba(16, 185, 129, 0.4);
}

.register-btn:active {
  transform: scale(0.98);
}

.register-footer {
  text-align: center;
  margin-top: 24px;
  padding-top: 20px;
  border-top: 1px solid #f1f5f9;
}

.footer-text {
  font-size: 13px;
  color: #94a3b8;
}

.login-link {
  font-size: 13px;
  font-weight: 600;
  color: #10b981;
  margin-left: 4px;
}

.login-link:hover {
  color: #059669;
}
</style>
