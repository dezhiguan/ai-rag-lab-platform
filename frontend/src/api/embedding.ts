import { http, type ApiResponse } from './http'
import type { EmbeddingRebuildResult, EmbeddingStatus } from '@/types/embedding'

export async function getEmbeddingStatus(kbId: number): Promise<ApiResponse<EmbeddingStatus>> {
  const { data } = await http.get<ApiResponse<EmbeddingStatus>>(`/api/kb/${kbId}/embedding/status`)
  return data
}

export async function rebuildEmbedding(kbId: number): Promise<ApiResponse<EmbeddingRebuildResult>> {
  const { data } = await http.post<ApiResponse<EmbeddingRebuildResult>>(`/api/kb/${kbId}/embedding/rebuild`)
  return data
}
