export interface TokenUsage {
  questionTokens: number
  contextTokens: number
  systemPromptTokens: number
  answerTokens: number
  inputTokens: number
  outputTokens: number
  totalTokens: number
  estimatedCost: number
  priceConfigured: boolean
  costNote?: string
  chatProvider?: string
  chatModel?: string
}

export interface TokenCostStats {
  queryCount: number
  totalTokens: number
  totalCost: number
  avgTokens: number
  avgCost: number
}

export interface TokenCostOverview {
  tokenTrackingEnabled: boolean
  capabilities: string[]
  allTime: TokenCostStats
  recent7Days: TokenCostStats
  modelPrices: {
    model: string
    inputPriceNote: string
    outputPriceNote: string
  }[]
}
