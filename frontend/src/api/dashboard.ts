import { http, type ApiResponse } from './http'

export interface DashboardStats {
  knowledgeBaseCount: number
  documentCount: number
  chunkCount: number
  sampleDataInitialized: boolean
}

export async function getDashboardStats(): Promise<ApiResponse<DashboardStats>> {
  const { data } = await http.get<ApiResponse<DashboardStats>>('/api/dashboard/stats')
  return data
}
