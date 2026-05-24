import type { SystemStatus } from '@/types/system'
import { http, type ApiResponse } from './http'

export interface HealthData {
  status: string
  appName: string
  version: string
  timestamp: string
}

export async function getHealth(): Promise<ApiResponse<HealthData>> {
  const { data } = await http.get<ApiResponse<HealthData>>('/api/system/health')
  return data
}

export async function getSystemStatus(): Promise<ApiResponse<SystemStatus>> {
  const { data } = await http.get<ApiResponse<SystemStatus>>('/api/system/status')
  return data
}
