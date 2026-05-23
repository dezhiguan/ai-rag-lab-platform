export interface RagQueryLogItem {
  queryLogId: number
  question: string
  searchMode: string
  enableRerank: boolean
  totalTimeMs: number
  retrievalTimeMs: number
  generationTimeMs: number
  createdAt: string
}

export interface RagQueryLogPage {
  total: number
  page: number
  pageSize: number
  records: RagQueryLogItem[]
}

export interface RagQueryLogQuery {
  page?: number
  pageSize?: number
  keyword?: string
  searchMode?: string
  enableRerank?: boolean
  slowOnly?: boolean
}
