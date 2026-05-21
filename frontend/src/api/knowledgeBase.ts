import { http, type ApiResponse } from './http'

export interface KnowledgeBase {
  id: number
  name: string
  description?: string
  status: string
  createdAt: string
  updatedAt: string
}

export interface KnowledgeBaseCreateRequest {
  name: string
  description?: string
}

export async function listKnowledgeBases(): Promise<ApiResponse<KnowledgeBase[]>> {
  const { data } = await http.get<ApiResponse<KnowledgeBase[]>>('/api/kb')
  return data
}

export async function getKnowledgeBase(id: number): Promise<ApiResponse<KnowledgeBase>> {
  const { data } = await http.get<ApiResponse<KnowledgeBase>>(`/api/kb/${id}`)
  return data
}

export async function createKnowledgeBase(
  payload: KnowledgeBaseCreateRequest
): Promise<ApiResponse<KnowledgeBase>> {
  const { data } = await http.post<ApiResponse<KnowledgeBase>>('/api/kb', payload)
  return data
}

export async function deleteKnowledgeBase(id: number): Promise<ApiResponse<void>> {
  const { data } = await http.delete<ApiResponse<void>>(`/api/kb/${id}`)
  return data
}
