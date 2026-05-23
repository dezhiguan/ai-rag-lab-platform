export type EvaluationSearchMode = 'VECTOR' | 'BM25' | 'HYBRID'

export interface EvaluationTestCase {
  caseId: string
  question: string
  expectedDocument: string
}

export interface EvaluationRunRequest {
  kbId: number
  searchMode?: EvaluationSearchMode
  topK?: number
}

export interface EvaluationCaseResult {
  caseId: string
  question: string
  expectedDocument: string
  actualTop1Document: string
  passed: boolean
  searchMode: string
  latencyMs: number
  message?: string
}

export interface EvaluationRunResult {
  kbId: number
  searchMode: string
  totalCount: number
  passedCount: number
  failedCount: number
  passRate: number
  totalLatencyMs: number
  results: EvaluationCaseResult[]
}
