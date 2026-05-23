<template>
  <div class="profile-page">
    <div class="page-header">
      <div class="page-header-left">
        <div class="page-icon">
          <UserOutlined />
        </div>
        <div>
          <h1 class="page-title">个人信息</h1>
          <p class="page-subtitle">管理您的账户信息</p>
        </div>
      </div>
    </div>

    <a-row :gutter="24">
      <a-col :xs="24" :lg="12">
        <div class="profile-card">
          <div class="card-header">
            <InfoCircleOutlined class="card-header-icon" />
            <span>账户信息</span>
          </div>
          <div class="card-body">
            <a-descriptions :column="1" size="large">
              <a-descriptions-item label="用户名">
                <span class="description-value">{{ userStore.user?.username }}</span>
              </a-descriptions-item>
              <a-descriptions-item label="邮箱">
                <span class="description-value">{{ userStore.user?.email || '未设置' }}</span>
              </a-descriptions-item>
              <a-descriptions-item label="注册时间">
                <span class="description-value">{{ userStore.user?.createdAt || '--' }}</span>
              </a-descriptions-item>
            </a-descriptions>
          </div>
        </div>
      </a-col>

      <a-col :xs="24" :lg="12">
        <div class="profile-card">
          <div class="card-header">
            <FormOutlined class="card-header-icon" />
            <span>编辑信息</span>
          </div>
          <div class="card-body">
            <a-form
              :model="formState"
              :rules="rules"
              layout="vertical"
              @finish="handleUpdate"
              class="profile-form"
            >
              <a-form-item name="username" label="用户名">
                <a-input
                  v-model:value="formState.username"
                  placeholder="请输入用户名"
                  size="large"
                >
                  <template #prefix>
                    <UserOutlined class="input-prefix" />
                  </template>
                </a-input>
              </a-form-item>

              <a-form-item name="email" label="邮箱">
                <a-input
                  v-model:value="formState.email"
                  placeholder="请输入邮箱"
                  size="large"
                >
                  <template #prefix>
                    <MailOutlined class="input-prefix" />
                  </template>
                </a-input>
              </a-form-item>

              <a-form-item class="form-submit">
                <a-button
                  type="primary"
                  html-type="submit"
                  :loading="loading"
                  block
                  size="large"
                  class="submit-btn"
                >
                  保存修改
                </a-button>
              </a-form-item>
            </a-form>
          </div>
        </div>
      </a-col>
    </a-row>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import { UserOutlined, MailOutlined, InfoCircleOutlined, FormOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import { useUserStore } from '../stores/user'

const userStore = useUserStore()
const loading = ref(false)

interface FormState {
  username: string
  email: string
}

const formState = reactive<FormState>({
  username: userStore.user?.username || '',
  email: userStore.user?.email || ''
})

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 2, message: '用户名至少 2 个字符', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入有效的邮箱地址', trigger: 'blur' }
  ]
}

onMounted(() => {
  // 同步表单数据
  formState.username = userStore.user?.username || ''
  formState.email = userStore.user?.email || ''
})

async function handleUpdate() {
  loading.value = true
  try {
    await userStore.updateUser({
      username: formState.username,
      email: formState.email
    })
    message.success('信息更新成功！')
  } catch (error: any) {
    const msg = error?.response?.data?.message || error?.message || '更新失败'
    message.error(msg)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.profile-page {
}

.page-header {
  margin-bottom: 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.page-header-left {
  display: flex;
  align-items: center;
  gap: 14px;
}

.page-icon {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  background: linear-gradient(135deg, #10b981, #059669);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 20px;
}

.page-title {
  font-size: 22px;
  font-weight: 700;
  color: #0f172a;
  margin: 0;
  letter-spacing: -0.02em;
}

.page-subtitle {
  font-size: 13px;
  color: #94a3b8;
  margin: 2px 0 0;
}

.profile-card {
  background: #ffffff;
  border-radius: 16px;
  box-shadow:
    0 1px 3px rgba(0, 0, 0, 0.04),
    0 8px 24px rgba(0, 0, 0, 0.06);
  overflow: hidden;
  margin-bottom: 24px;
}

.card-header {
  padding: 20px 24px 16px;
  border-bottom: 1px solid #f1f5f9;
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 15px;
  font-weight: 600;
  color: #0f172a;
}

.card-header-icon {
  color: #10b981;
  font-size: 18px;
}

.card-body {
  padding: 24px;
}

.description-value {
  font-weight: 500;
  color: #0f172a;
}

.profile-form :deep(.ant-form-item) {
  margin-bottom: 20px;
}

.profile-form :deep(.ant-form-item-label) {
  padding-bottom: 6px;
}

.profile-form :deep(.ant-form-item-label label) {
  font-size: 14px;
  font-weight: 500;
  color: #334155;
  height: auto;
}

.profile-form :deep(.ant-input-affix-wrapper) {
  border-radius: 10px;
  border: 1.5px solid #e2e8f0;
  padding: 8px 14px;
  font-size: 14px;
  transition: all 0.2s ease;
  background: #fff;
  color: #0f172a;
}

.profile-form :deep(.ant-input-affix-wrapper:hover) {
  border-color: #94a3b8;
}

.profile-form :deep(.ant-input-affix-wrapper-focused) {
  border-color: #10b981;
  box-shadow: 0 0 0 3px rgba(16, 185, 129, 0.1);
}

.profile-form :deep(.ant-form-item-explain-error) {
  font-size: 12px;
  margin-top: 4px;
}

.profile-form :deep(.ant-input-affix-wrapper > .ant-input) {
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

.submit-btn {
  height: 46px;
  font-size: 15px;
  font-weight: 600;
  border-radius: 10px;
  border: none;
  background: linear-gradient(135deg, #10b981, #059669);
  box-shadow: 0 4px 12px rgba(16, 185, 129, 0.3);
  transition: all 0.2s ease;
}

.submit-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 16px rgba(16, 185, 129, 0.4);
}

.submit-btn:active {
  transform: scale(0.98);
}
</style>
