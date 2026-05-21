import { http, type ApiResponse } from './http'

export interface DocumentItem {
  id: number
  kbId: number
  fileName: string
  fileType: string
  fileSize?: number
  contentHash?: string
  status: string
  errorMessage?: string
  createdAt: string
  updatedAt: string
}

export interface DocumentChunk {
  id: number
  kbId: number
  documentId: number
  chunkIndex: number
  titlePath?: string
  content: string
  tokenCount: number
  contentHash: string
  createdAt: string
}

export async function listDocuments(kbId: number): Promise<ApiResponse<DocumentItem[]>> {
  const { data } = await http.get<ApiResponse<DocumentItem[]>>(`/api/kb/${kbId}/documents`)
  return data
}

export async function getDocument(documentId: number): Promise<ApiResponse<DocumentItem>> {
  const { data } = await http.get<ApiResponse<DocumentItem>>(`/api/documents/${documentId}`)
  return data
}

export async function listChunks(documentId: number): Promise<ApiResponse<DocumentChunk[]>> {
  const { data } = await http.get<ApiResponse<DocumentChunk[]>>(
    `/api/documents/${documentId}/chunks`
  )
  return data
}

export async function uploadDocument(
  kbId: number,
  file: File
): Promise<ApiResponse<DocumentItem>> {
  const formData = new FormData()
  formData.append('file', file)
  const { data } = await http.post<ApiResponse<DocumentItem>>(
    `/api/kb/${kbId}/documents/upload`,
    formData,
    { headers: { 'Content-Type': 'multipart/form-data' } }
  )
  return data
}
