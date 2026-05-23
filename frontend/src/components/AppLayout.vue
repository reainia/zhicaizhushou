<template>
  <div class="app-layout">
    <!-- Sidebar -->
    <div class="sidebar" :class="{ 'sidebar-collapsed': collapsed }">
      <div class="sidebar-header">
        <div class="sidebar-logo">
          <div class="logo-icon">
            <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M12 2v20M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6" />
            </svg>
          </div>
          <span v-show="!collapsed" class="sidebar-title">智财助手</span>
        </div>
      </div>

      <div class="sidebar-nav">
        <router-link
          v-for="item in navItems"
          :key="item.path"
          :to="item.path"
          class="nav-item"
          :class="{ 'nav-item-active': isActive(item.path) }"
        >
          <div class="nav-item-icon">
            <component :is="item.icon" />
          </div>
          <span v-show="!collapsed" class="nav-item-label">{{ item.label }}</span>
        </router-link>
      </div>

      <div class="sidebar-footer">
        <div class="sidebar-user">
          <div class="user-avatar">{{ userStore.user?.username?.[0] || 'U' }}</div>
          <div v-show="!collapsed" class="user-info">
            <div class="user-name">{{ userStore.user?.username || '用户' }}</div>
            <div class="user-role">个人用户</div>
          </div>
        </div>
        <a-button
          type="text"
          class="logout-btn"
          :class="{ 'logout-btn-collapsed': collapsed }"
          @click="handleLogout"
        >
          <template #icon><LogoutOutlined /></template>
          <span v-show="!collapsed">退出登录</span>
        </a-button>
      </div>

      <!-- Collapse toggle -->
      <button class="collapse-btn" @click="collapsed = !collapsed">
        <MenuFoldOutlined v-if="!collapsed" />
        <MenuUnfoldOutlined v-else />
      </button>
    </div>

    <!-- Main Content -->
    <div class="main-area" :class="{ 'main-expanded': collapsed }">
      <div class="main-content">
        <router-view />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import {
  DashboardOutlined,
  OrderedListOutlined,
  PieChartOutlined,
  GiftOutlined,
  UserOutlined,
  LogoutOutlined,
  MenuFoldOutlined,
  MenuUnfoldOutlined
} from '@ant-design/icons-vue'
import { Modal } from 'ant-design-vue'
import { useUserStore } from '../stores/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const collapsed = ref(false)

const navItems = [
  { path: '/dashboard', label: '仪表盘', icon: DashboardOutlined },
  { path: '/transactions', label: '交易记录', icon: OrderedListOutlined },
  { path: '/budget', label: '预算管理', icon: PieChartOutlined },
  { path: '/social-gifts', label: '人情账单', icon: GiftOutlined },
  { path: '/profile', label: '个人信息', icon: UserOutlined }
]

function isActive(path: string): boolean {
  return route.path === path
}

function handleLogout() {
  Modal.confirm({
    title: '确认退出',
    content: '确定要退出登录吗？',
    okText: '退出',
    cancelText: '取消',
    okButtonProps: { danger: true },
    onOk: () => {
      userStore.logout()
      router.push('/login')
    }
  })
}
</script>

<style scoped>
.app-layout {
  display: flex;
  min-height: 100dvh;
  background: #f8fafc;
}

/* Sidebar */
.sidebar {
  width: 240px;
  background: #ffffff;
  border-right: 1px solid #f1f5f9;
  display: flex;
  flex-direction: column;
  position: fixed;
  top: 0;
  left: 0;
  bottom: 0;
  z-index: 100;
  transition: width 0.2s ease;
}

.sidebar-collapsed {
  width: 64px;
}

.sidebar-header {
  padding: 20px 16px;
  border-bottom: 1px solid #f1f5f9;
}

.sidebar-logo {
  display: flex;
  align-items: center;
  gap: 10px;
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
  flex-shrink: 0;
}

.sidebar-title {
  font-size: 17px;
  font-weight: 700;
  color: #0f172a;
  letter-spacing: -0.01em;
  white-space: nowrap;
}

/* Navigation */
.sidebar-nav {
  flex: 1;
  padding: 12px 8px;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border-radius: 10px;
  color: #64748b;
  text-decoration: none;
  transition: all 0.15s ease;
  font-size: 14px;
  font-weight: 500;
}

.nav-item:hover {
  background: #f8fafc;
  color: #0f172a;
}

.nav-item-active {
  background: rgba(16, 185, 129, 0.08);
  color: #10b981;
  font-weight: 600;
}

.nav-item-active:hover {
  background: rgba(16, 185, 129, 0.12);
  color: #10b981;
}

.nav-item-icon {
  width: 22px;
  height: 22px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  font-size: 18px;
}

.nav-item-label {
  white-space: nowrap;
}

/* Footer */
.sidebar-footer {
  padding: 12px 8px;
  border-top: 1px solid #f1f5f9;
}

.sidebar-user {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  margin-bottom: 8px;
}

.user-avatar {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  background: linear-gradient(135deg, #10b981, #059669);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 700;
  flex-shrink: 0;
}

.user-info {
  flex: 1;
  min-width: 0;
}

.user-name {
  font-size: 13px;
  font-weight: 600;
  color: #0f172a;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.user-role {
  font-size: 11px;
  color: #94a3b8;
}

.logout-btn {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: flex-start;
  gap: 10px;
  padding: 8px 12px;
  color: #94a3b8;
  font-size: 13px;
  border-radius: 10px;
  height: auto;
}

.logout-btn:hover {
  color: #ef4444 !important;
  background: rgba(239, 68, 68, 0.06) !important;
}

.logout-btn-collapsed {
  justify-content: center;
  padding: 8px;
}

/* Collapse button */
.collapse-btn {
  position: absolute;
  top: 50%;
  right: -12px;
  transform: translateY(-50%);
  width: 24px;
  height: 24px;
  border-radius: 50%;
  border: 1px solid #f1f5f9;
  background: #fff;
  color: #94a3b8;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  font-size: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
  transition: all 0.15s ease;
  z-index: 1;
}

.collapse-btn:hover {
  color: #0f172a;
  border-color: #e2e8f0;
}

/* Main area */
.main-area {
  flex: 1;
  margin-left: 240px;
  transition: margin-left 0.2s ease;
}

.main-expanded {
  margin-left: 64px;
}

.main-content {
  padding: 32px;
}
</style>
