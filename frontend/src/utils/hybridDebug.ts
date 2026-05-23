import type { DebugRetrievedChunk } from '@/types/debug'

export function hybridSourceLabel(chunk: DebugRetrievedChunk): string {
  const vector = chunk.matchedByVector === true
  const bm25 = chunk.matchedByBm25 === true
  if (vector && bm25) return 'Vector + BM25'
  if (vector) return 'Vector'
  if (bm25) return 'BM25'
  return '—'
}

export function hasHybridObservability(
  searchMode?: string,
  chunk?: DebugRetrievedChunk
): boolean {
  return (
    searchMode === 'HYBRID' &&
    (chunk?.hybridScore != null ||
      chunk?.vectorScore != null ||
      chunk?.bm25Score != null ||
      chunk?.matchedByVector != null ||
      chunk?.matchedByBm25 != null)
  )
}

export function formatOptionalScore(score?: number | null): string {
  if (score == null || Number.isNaN(score)) return '—'
  return score.toFixed(4)
}
