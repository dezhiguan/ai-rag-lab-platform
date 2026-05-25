import { http, type ApiResponse } from './http'
import type { TokenCostOverview } from '@/types/token'

export async function getTokenCostOverview(): Promise<ApiResponse<TokenCostOverview>> {
  const { data } = await http.get<ApiResponse<TokenCostOverview>>('/api/token-cost/overview')
  return data
}
