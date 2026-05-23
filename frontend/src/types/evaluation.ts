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
  enableRerank?: boolean
}

export interface EvaluationCompareRequest {
  kbId: number
  topK?: number
  enableRerank?: boolean
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
  enableRerank?: boolean
  totalCount: number
  passedCount: number
  failedCount: number
  passRate: number
  totalLatencyMs: number
  avgLatencyMs?: number
  results: EvaluationCaseResult[]
}

export interface EvaluationModeHit {
  searchMode?: string
  actualTop1Document: string
  passed: boolean
  latencyMs: number
}

export interface EvaluationModeSummary {
  searchMode: string
  totalCount: number
  passedCount: number
  failedCount: number
  passRate: number
  totalLatencyMs: number
  avgLatencyMs: number
}

export interface EvaluationCompareCase {
  caseId: string
  question: string
  expectedDocument: string
  vector?: EvaluationModeHit
  bm25?: EvaluationModeHit
  hybrid?: EvaluationModeHit
}

export interface EvaluationCompareResult {
  kbId: number
  enableRerank?: boolean
  modeSummaries: EvaluationModeSummary[]
  cases: EvaluationCompareCase[]
}
