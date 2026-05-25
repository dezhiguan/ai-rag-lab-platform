export interface DebugRetrievedChunk {
  documentId: number
  documentName: string
  chunkId: number
  chunkIndex: number
  score: number
  rankPosition: number
  content: string
  usedInPrompt?: boolean
  filterReason?: string | null
  /** HYBRID 模式观测字段 */
  matchedByVector?: boolean | null
  matchedByBm25?: boolean | null
  vectorScore?: number | null
  bm25Score?: number | null
  hybridScore?: number | null
  originalRank?: number | null
  rerankRank?: number | null
  rerankScore?: number | null
}

import type { TokenUsage } from './token'

export interface DebugLatency {
  retrievalTimeMs: number
  generationTimeMs: number
  totalTimeMs: number
}

export type DebugSearchMode = 'VECTOR' | 'BM25' | 'HYBRID'

export interface DebugQueryRequest {
  kbId: number
  question: string
  topK?: number
  searchMode?: DebugSearchMode
  enableRerank?: boolean
}

export interface DebugQueryResult {
  queryLogId: number
  kbId: number
  question: string
  searchMode?: string
  enableRerank?: boolean
  embeddingProvider: string
  embeddingModel: string
  chatProvider: string
  chatModel: string
  retrievedChunks: DebugRetrievedChunk[]
  contextChunks: DebugRetrievedChunk[]
  context: string
  prompt: string
  answer: string
  latency: DebugLatency
  tokenUsage?: TokenUsage
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
