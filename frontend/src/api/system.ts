import axios from 'axios'
import type { SystemStatus } from '@/types/system'

export interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

export interface HealthData {
  status: string
  appName: string
  version: string
  timestamp: string
}

const http = axios.create({
  baseURL: '',
  timeout: 10000,
})

export async function getHealth(): Promise<ApiResponse<HealthData>> {
  const { data } = await http.get<ApiResponse<HealthData>>('/api/system/health')
  return data
}

export async function getSystemStatus(): Promise<ApiResponse<SystemStatus>> {
  const { data } = await http.get<ApiResponse<SystemStatus>>('/api/system/status')
  return data
}
