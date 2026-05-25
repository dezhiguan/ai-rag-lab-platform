import axios from 'axios'
import { ElMessage } from 'element-plus'
import { API_BASE_URL } from '@/config/api'
import { TOKEN_STORAGE_KEY } from '@/types/auth'
import { GUEST_WRITE_DENIED_MSG } from '@/constants/permission'

export interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

export const http = axios.create({
  baseURL: API_BASE_URL,
  timeout: 30000,
})

http.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_STORAGE_KEY)
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

http.interceptors.response.use(
  (response) => {
    if (response.data?.code === 401) {
      localStorage.removeItem(TOKEN_STORAGE_KEY)
      if (!window.location.pathname.startsWith('/login')) {
        window.location.href = '/login'
      }
    }
    if (response.data?.code === 403) {
      ElMessage.warning(response.data?.message || GUEST_WRITE_DENIED_MSG)
    }
    return response
  },
  (error) => {
    const code = error.response?.data?.code
    const status = error.response?.status
    const message = error.response?.data?.message
    if (code === 401 || status === 401) {
      localStorage.removeItem(TOKEN_STORAGE_KEY)
      if (!window.location.pathname.startsWith('/login')) {
        window.location.href = '/login'
      }
    }
    if (code === 403 || status === 403) {
      ElMessage.warning(message || GUEST_WRITE_DENIED_MSG)
    }
    return Promise.reject(error)
  },
)
