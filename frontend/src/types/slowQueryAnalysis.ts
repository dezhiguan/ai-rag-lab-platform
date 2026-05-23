export interface SlowQueryItem {
  queryLogId: number
  question: string
  searchMode: string
  enableRerank: boolean
  retrievalTimeMs: number
  generationTimeMs: number
  totalTimeMs: number
  retrievalChunkCount: number
  slowReasons: string[]
  suggestions: string[]
  createdAt: string
}

export interface SlowQueryAnalysis {
  totalCount: number
  records: SlowQueryItem[]
}
