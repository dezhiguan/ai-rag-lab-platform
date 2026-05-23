import { http, type ApiResponse } from './http'
import type { RagMetrics } from '@/types/ragMetrics'

export async function getRagMetrics(): Promise<ApiResponse<RagMetrics>> {
  const { data } = await http.get<ApiResponse<RagMetrics>>('/api/rag/metrics')
  return data
}
