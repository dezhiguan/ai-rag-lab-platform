export interface TokenUsage {
  questionTokens: number
  contextTokens: number
  systemPromptTokens: number
  answerTokens: number
  inputTokens: number
  outputTokens: number

  embeddingProvider?: string
  embeddingModel?: string
  embeddingTokens?: number
  embeddingCost?: number

  chatProvider?: string
  chatModel?: string
  chatInputTokens?: number
  chatOutputTokens?: number
  chatCost?: number

  totalTokens: number
  totalCost?: number
  /** 兼容 V11-01 */
  estimatedCost?: number
  priceConfigured?: boolean
  costNote?: string
}

export interface TokenCostStats {
  queryCount: number
  totalTokens: number
  totalCost: number
  avgTokens: number
  avgCost: number
}

export interface EmbeddingCostStats {
  queryCount: number
  embeddingTokens: number
  embeddingCost: number
  avgEmbeddingTokens: number
  avgEmbeddingCost: number
  embeddingProvider?: string
  embeddingModel?: string
}

export interface TokenCostOverview {
  tokenTrackingEnabled: boolean
  capabilities: string[]
  allTime: TokenCostStats
  recent7Days: TokenCostStats
  embeddingAllTime?: EmbeddingCostStats
  embeddingRecent7Days?: EmbeddingCostStats
  modelPrices: {
    model: string
    inputPriceNote: string
    outputPriceNote: string
  }[]
}
