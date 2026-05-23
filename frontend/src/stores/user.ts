import { defineStore } from 'pinia'
import { ref } from 'vue'
import api from '../api/auth'

export interface User {
  id: number
  username: string
  email: string
  createdAt?: string
  avatarUrl?: string
}

export interface AuthResponse {
  token: string
  user: User
}

export const useUserStore = defineStore('user', () => {
  const token = ref<string>(localStorage.getItem('token') || '')
  const user = ref<User | null>(null)

  const isAuthenticated = ref<boolean>(!!localStorage.getItem('token'))

  function setToken(newToken: string) {
    token.value = newToken
    localStorage.setItem('token', newToken)
    isAuthenticated.value = true
  }

  function setUser(newUser: User) {
    user.value = newUser
    localStorage.setItem('user', JSON.stringify(newUser))
  }

  function logout() {
    token.value = ''
    user.value = null
    isAuthenticated.value = false
    localStorage.removeItem('token')
    localStorage.removeItem('user')
  }

  // 初始化时从 localStorage 恢复用户信息
  function init() {
    const savedUser = localStorage.getItem('user')
    if (savedUser) {
      try {
        user.value = JSON.parse(savedUser)
      } catch {
        user.value = null
      }
    }
  }

  // 更新用户信息
  async function updateUser(info: Partial<User>) {
    const res = await api.put<User>('/auth/profile', info)
    setUser(res.data)
    return res.data
  }

  // 从服务器获取用户信息
  async function fetchProfile() {
    const res = await api.get<User>('/auth/profile')
    setUser(res.data)
    return res.data
  }

  return {
    token,
    user,
    isAuthenticated,
    setToken,
    setUser,
    logout,
    init,
    updateUser,
    fetchProfile
  }
})