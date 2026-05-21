import { http, type ApiResponse } from './http'

export interface SampleStatus {
  initialized: boolean
  knowledgeBaseId?: number
  documentCount: number
}

export interface SampleInitResult {
  initialized: boolean
  knowledgeBaseId?: number
  documentCount: number
  message: string
}

export async function getSampleStatus(): Promise<ApiResponse<SampleStatus>> {
  const { data } = await http.get<ApiResponse<SampleStatus>>('/api/sample/status')
  return data
}

export async function initSampleData(): Promise<ApiResponse<SampleInitResult>> {
  const { data } = await http.post<ApiResponse<SampleInitResult>>('/api/sample/init')
  return data
}
