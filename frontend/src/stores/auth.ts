import { defineStore } from 'pinia'
import { fetchMe, login as loginApi, logout as logoutApi } from '@/api/auth'
import { TOKEN_STORAGE_KEY, type AuthUser } from '@/types/auth'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem(TOKEN_STORAGE_KEY) ?? '',
    user: null as AuthUser | null,
    initialized: false,
  }),
  getters: {
    isLoggedIn: (state) => !!state.token,
    username: (state) => state.user?.username ?? '',
    roleLabel: (state) => state.user?.roleLabel ?? '',
    modeLabel: (state) => state.user?.mode ?? '',
    isAdmin: (state) => state.user?.role === 'ADMIN',
    canWrite: (state) => state.user?.role === 'ADMIN',
    isGuest: (state) => state.user?.role === 'GUEST',
  },
  actions: {
    setSession(token: string, user: AuthUser) {
      this.token = token
      this.user = user
      localStorage.setItem(TOKEN_STORAGE_KEY, token)
    },
    clearSession() {
      this.token = ''
      this.user = null
      localStorage.removeItem(TOKEN_STORAGE_KEY)
    },
    async login(username: string, password: string) {
      const res = await loginApi({ username, password })
      if (res.code !== 200 || !res.data) {
        throw new Error(res.message || '登录失败')
      }
      const { token, ...user } = res.data
      this.setSession(token, user)
      return user
    },
    async restoreSession() {
      if (!this.token) {
        this.initialized = true
        return false
      }
      try {
        const res = await fetchMe()
        if (res.code !== 200 || !res.data) {
          this.clearSession()
          this.initialized = true
          return false
        }
        this.user = res.data
        this.initialized = true
        return true
      } catch {
        this.clearSession()
        this.initialized = true
        return false
      }
    },
    async logout() {
      try {
        await logoutApi()
      } catch {
        // ignore network errors on logout
      } finally {
        this.clearSession()
      }
    },
  },
})
