import { http, type ApiResponse } from './http'

export interface EsIndexRebuildResult {
  indexName: string
  syncedCount: number
}

export interface Bm25SearchResultItem {
  documentId: number
  documentName: string
  chunkId: number
  chunkIndex: number
  score: number
  content: string
  matchedTerms?: string[]
}

export interface Bm25SearchResult {
  query: string
  extractedTerms?: string[]
  results: Bm25SearchResultItem[]
}

export interface Bm25SearchRequest {
  kbId: number
  query: string
  topK?: number
}

export async function rebuildSearchIndex(): Promise<ApiResponse<EsIndexRebuildResult>> {
  const { data } = await http.post<ApiResponse<EsIndexRebuildResult>>('/api/search/index/rebuild')
  return data
}

export async function bm25Search(
  payload: Bm25SearchRequest
): Promise<ApiResponse<Bm25SearchResult>> {
  const { data } = await http.post<ApiResponse<Bm25SearchResult>>('/api/search/bm25', payload)
  return data
}
