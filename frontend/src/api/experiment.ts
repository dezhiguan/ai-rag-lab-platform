import { http, type ApiResponse } from './http'
import type { ExperimentRagQueryRequest, ExperimentRagQueryResult } from '@/types/experiment'

export async function runRagExperiment(
  payload: ExperimentRagQueryRequest
): Promise<ApiResponse<ExperimentRagQueryResult>> {
  const { data } = await http.post<ApiResponse<ExperimentRagQueryResult>>(
    '/api/experiment/rag-query',
    payload
  )
  return data
}
