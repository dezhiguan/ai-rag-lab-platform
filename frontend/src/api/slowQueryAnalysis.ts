import { http, type ApiResponse } from './http'
import type { SlowQueryAnalysis } from '@/types/slowQueryAnalysis'

export async function getSlowQueryAnalysis(
  limit?: number
): Promise<ApiResponse<SlowQueryAnalysis>> {
  const { data } = await http.get<ApiResponse<SlowQueryAnalysis>>('/api/rag/query-logs/slow-analysis', {
    params: limit ? { limit } : undefined,
  })
  return data
}
