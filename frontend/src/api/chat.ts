import { http, type ApiResponse } from './http'
import type { ChatMessage, ChatRequest, ChatResponse, ChatSession } from '@/types/chat'

export async function sendChat(payload: ChatRequest): Promise<ApiResponse<ChatResponse>> {
  const { data } = await http.post<ApiResponse<ChatResponse>>('/api/chat', payload)
  return data
}

export async function listChatSessions(kbId?: number): Promise<ApiResponse<ChatSession[]>> {
  const { data } = await http.get<ApiResponse<ChatSession[]>>('/api/chat/sessions', {
    params: kbId != null ? { kbId } : undefined,
  })
  return data
}

export async function listChatMessages(sessionId: number): Promise<ApiResponse<ChatMessage[]>> {
  const { data } = await http.get<ApiResponse<ChatMessage[]>>(`/api/chat/sessions/${sessionId}/messages`)
  return data
}
