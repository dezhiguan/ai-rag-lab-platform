import { http, type ApiResponse } from './http'
import type { DebugQueryLogSummary, DebugQueryRequest, DebugQueryResult } from '@/types/debug'

export async function executeDebugQuery(
  payload: DebugQueryRequest
): Promise<ApiResponse<DebugQueryResult>> {
  const { data } = await http.post<ApiResponse<DebugQueryResult>>('/api/debug/query', payload)
  return data
}

export async function listDebugQueryLogs(
  kbId?: number
): Promise<ApiResponse<DebugQueryLogSummary[]>> {
  const { data } = await http.get<ApiResponse<DebugQueryLogSummary[]>>('/api/debug/query-logs', {
    params: kbId != null ? { kbId } : undefined,
  })
  return data
}

export async function getDebugQueryLog(
  queryLogId: number
): Promise<ApiResponse<DebugQueryResult>> {
  const { data } = await http.get<ApiResponse<DebugQueryResult>>(
    `/api/debug/query-logs/${queryLogId}`
  )
  return data
}
