import axios from 'axios'
import { API_BASE_URL } from '@/config/api'

export interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

export const http = axios.create({
  baseURL: API_BASE_URL,
  timeout: 30000,
})
