import axios from 'axios'
import { API_BASE_URL } from '@/config/api'
import { TOKEN_STORAGE_KEY } from '@/types/auth'

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
    return response
  },
  (error) => {
    const code = error.response?.data?.code
    const status = error.response?.status
    if (code === 401 || status === 401) {
      localStorage.removeItem(TOKEN_STORAGE_KEY)
      if (!window.location.pathname.startsWith('/login')) {
        window.location.href = '/login'
      }
    }
    return Promise.reject(error)
  },
)
