import axios from 'axios'

export interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

export const http = axios.create({
  baseURL: '',
  timeout: 30000,
})
