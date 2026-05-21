export interface EmbeddingStatus {
  totalChunks: number
  embeddedChunks: number
  notEmbeddedChunks: number
}

export interface EmbeddingRebuildResult {
  totalChunks: number
  embeddedChunks: number
  embeddingModel: string
  embeddingDimension: number
}
