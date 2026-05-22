export interface DebugRetrievedChunk {
  documentId: number
  documentName: string
  chunkId: number
  chunkIndex: number
  score: number
  rankPosition: number
  content: string
}

export interface DebugLatency {
  retrievalTimeMs: number
  generationTimeMs: number
  totalTimeMs: number
}

export interface DebugQueryRequest {
  kbId: number
  question: string
  topK?: number
}

export interface DebugQueryResult {
  queryLogId: number
  kbId: number
  question: string
  embeddingProvider: string
  embeddingModel: string
  chatProvider: string
  chatModel: string
  retrievedChunks: DebugRetrievedChunk[]
  context: string
  prompt: string
  answer: string
  latency: DebugLatency
}

export interface DebugQueryLogSummary {
  queryLogId: number
  kbId: number
  question: string
  embeddingProvider: string
  embeddingModel: string
  chatProvider: string
  chatModel: string
  topK: number
  retrievalTimeMs: number
  generationTimeMs: number
  totalTimeMs: number
  createdAt: string
}
