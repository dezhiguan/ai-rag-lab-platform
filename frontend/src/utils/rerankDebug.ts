import type { DebugRetrievedChunk } from '@/types/debug'

/** 排名变化 = 原排名 − 重排排名；正数表示上升 */
export function rerankRankDelta(chunk: DebugRetrievedChunk): number | null {
  const original = chunk.originalRank
  const rerank = chunk.rerankRank ?? chunk.rankPosition
  if (original == null || rerank == null) return null
  return original - rerank
}

export function rerankChangeLabel(chunk: DebugRetrievedChunk): string {
  const delta = rerankRankDelta(chunk)
  if (delta == null) return '—'
  if (delta > 0) return `上升 +${delta}`
  if (delta < 0) return `下降 ${delta}`
  return '不变'
}

export type RerankChangeKind = 'up' | 'down' | 'same' | 'unknown'

export function rerankChangeKind(chunk: DebugRetrievedChunk): RerankChangeKind {
  const delta = rerankRankDelta(chunk)
  if (delta == null) return 'unknown'
  if (delta > 0) return 'up'
  if (delta < 0) return 'down'
  return 'same'
}

export function rerankChangeTagType(
  kind: RerankChangeKind
): 'success' | 'danger' | 'info' | '' {
  if (kind === 'up') return 'success'
  if (kind === 'down') return 'danger'
  if (kind === 'same') return 'info'
  return ''
}

export function hasRerankObservability(
  enableRerank?: boolean,
  chunks?: DebugRetrievedChunk[]
): boolean {
  if (enableRerank === true) return true
  return (chunks ?? []).some(
    (c) => c.originalRank != null || c.rerankRank != null || c.rerankScore != null
  )
}

/** 启用重排时按 rerankRank 排序展示 */
export function sortChunksForDisplay(
  chunks: DebugRetrievedChunk[],
  enableRerank?: boolean
): DebugRetrievedChunk[] {
  if (!enableRerank || !hasRerankObservability(enableRerank, chunks)) {
    return chunks
  }
  return [...chunks].sort((a, b) => {
    const ra = a.rerankRank ?? a.rankPosition ?? 0
    const rb = b.rerankRank ?? b.rankPosition ?? 0
    return ra - rb
  })
}

export function summarizeRerankChanges(chunks: DebugRetrievedChunk[]): string {
  let up = 0
  let down = 0
  let same = 0
  for (const c of chunks) {
    const kind = rerankChangeKind(c)
    if (kind === 'up') up++
    else if (kind === 'down') down++
    else if (kind === 'same') same++
  }
  const parts: string[] = []
  if (up > 0) parts.push(`${up} 条上升`)
  if (down > 0) parts.push(`${down} 条下降`)
  if (same > 0) parts.push(`${same} 条不变`)
  return parts.length ? parts.join('，') : '无排名变化'
}
