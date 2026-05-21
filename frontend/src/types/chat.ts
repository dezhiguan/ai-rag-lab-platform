export interface ChatSource {
  documentId: number
  documentName: string
  chunkId: number
  chunkIndex: number
  score: number
  content: string
}

export interface ChatRequest {
  kbId: number
  sessionId?: number
  question: string
  topK?: number
}

export interface ChatResponse {
  sessionId: number
  answer: string
  sources: ChatSource[]
}

export interface ChatSession {
  id: number
  kbId: number
  title: string
  createdAt: string
  updatedAt: string
}

export interface ChatMessage {
  id: number
  sessionId: number
  kbId: number
  role: string
  content: string
  sources?: ChatSource[]
  promptTokens?: number
  completionTokens?: number
  latencyMs?: number
  createdAt: string
}
