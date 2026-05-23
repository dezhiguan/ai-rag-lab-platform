export interface RagMetrics {
  totalQueryCount: number
  todayQueryCount: number
  avgTotalTimeMs: number
  avgRetrievalTimeMs: number
  avgGenerationTimeMs: number
  searchModeStats: SearchModeStats
  rerankStats: RerankStats
  slowQueries: SlowQueryItem[]
}

export interface SearchModeStats {
  vectorCount: number
  bm25Count: number
  hybridCount: number
}

export interface RerankStats {
  enabledCount: number
  disabledCount: number
}

export interface SlowQueryItem {
  queryLogId: number
  question: string
  searchMode: string
  enableRerank: boolean
  totalTimeMs: number
  createdAt: string
}
