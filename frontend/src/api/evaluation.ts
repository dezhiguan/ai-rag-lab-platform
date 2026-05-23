import { http, type ApiResponse } from './http'
import type {
  EvaluationRunRequest,
  EvaluationRunResult,
  EvaluationTestCase,
} from '@/types/evaluation'

export async function listEvaluationCases(): Promise<ApiResponse<EvaluationTestCase[]>> {
  const { data } = await http.get<ApiResponse<EvaluationTestCase[]>>('/api/evaluation/cases')
  return data
}

export async function runEvaluation(
  payload: EvaluationRunRequest
): Promise<ApiResponse<EvaluationRunResult>> {
  const { data } = await http.post<ApiResponse<EvaluationRunResult>>('/api/evaluation/run', payload)
  return data
}
