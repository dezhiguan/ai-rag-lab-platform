import { http, type ApiResponse } from './http'

export interface ModelProviders {
  embeddingProvider: string
  embeddingModel: string
  embeddingDimension: number
  embeddingDelegate: string
  chatProvider: string
  chatModel: string
  chatDelegate: string
}

export async function getModelProviders(): Promise<ApiResponse<ModelProviders>> {
  const { data } = await http.get<ApiResponse<ModelProviders>>('/api/model/providers')
  return data
}
