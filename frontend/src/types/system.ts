export interface SystemStatus {
  backend: BackendStatus
  postgresql: DependencyStatus
  elasticsearch: ElasticsearchStatus
  modelProvider: ModelProviderStatus
  features: FeatureStatus[]
}

export interface BackendStatus {
  status: string
  appName: string
  version: string
  serverTime: string
  message?: string
}

export interface DependencyStatus {
  name: string
  status: string
  message?: string
}

export interface ElasticsearchStatus {
  status: string
  indexName: string
  message?: string
}

export interface ModelProviderStatus {
  embeddingProvider: string
  embeddingModel: string
  chatProvider: string
  chatModel: string
}

export interface FeatureStatus {
  key: string
  label: string
  status: string
}
