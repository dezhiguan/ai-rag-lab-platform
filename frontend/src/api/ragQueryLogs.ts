import { http, type ApiResponse } from './http'
import type { RagQueryLogPage, RagQueryLogQuery } from '@/types/ragQueryLogs'

export async function listRagQueryLogs(
  params: RagQueryLogQuery
): Promise<ApiResponse<RagQueryLogPage>> {
  const { data } = await http.get<ApiResponse<RagQueryLogPage>>('/api/rag/query-logs', { params })
  return data
}
