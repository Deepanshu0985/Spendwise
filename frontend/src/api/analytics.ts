import { apiClient } from './client'
import type {
  AnalyticsMeta,
  CategoryBreakdownEntry,
  MerchantBreakdownEntry,
  MonthlySummary,
  TrendPoint,
} from './types'

export interface AnalyticsParams {
  from: string
  to: string
  currency?: string
  [key: string]: string | number | undefined
}

export const analyticsApi = {
  monthly: (params: AnalyticsParams) => apiClient.getWithMeta<MonthlySummary, AnalyticsMeta>('/analytics/monthly', params),
  categories: (params: AnalyticsParams) =>
    apiClient.getWithMeta<CategoryBreakdownEntry[], AnalyticsMeta>('/analytics/categories', params),
  merchants: (params: AnalyticsParams) =>
    apiClient.getWithMeta<MerchantBreakdownEntry[], AnalyticsMeta>('/analytics/merchants', params),
  trends: (params: AnalyticsParams) => apiClient.getWithMeta<TrendPoint[], AnalyticsMeta>('/analytics/trends', params),
}
