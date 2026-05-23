import type { DebugRetrievedChunk, DebugLatency } from '@/types/debug'

export interface ExperimentRagQueryRequest {
  kbId: number
  question: string
  topK?: number
  searchMode?: 'VECTOR' | 'BM25' | 'HYBRID'
  enableRerank?: boolean
  maxChunks?: number
  minScore?: number
  maxScoreGap?: number
}

export interface ExperimentParams {
  topK: number
  searchMode: string
  enableRerank: boolean
  maxChunks: number
  minScore: number
  maxScoreGap: number
}

export interface ExperimentImpact {
  retrievedCount: number
  contextCount: number
  filteredCount: number
}

export interface ExperimentRagQueryResult {
  queryLogId?: number
  kbId: number
  question: string
  answer: string
  prompt: string
  context: string
  retrievedChunks: DebugRetrievedChunk[]
  contextChunks: DebugRetrievedChunk[]
  latency: DebugLatency
  usedParams: ExperimentParams
  impact: ExperimentImpact
}

/** 页面内实验对比条目（不落库） */
export interface ExperimentCompareEntry {
  id: number
  label: string
  result: ExperimentRagQueryResult
  addedAt: string
}
