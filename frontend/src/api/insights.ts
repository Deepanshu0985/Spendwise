import { apiClient } from './client'
import type { MonthlyInsight } from './types'

export const insightsApi = {
  // The insight already written for a month, or null. Never calls the AI.
  get: (month: string) => apiClient.get<MonthlyInsight | null>(`/ai/insights/monthly?month=${month}`),
  // Writes the month's insight, or returns the stored one if the figures have not changed.
  generate: (month: string) => apiClient.post<MonthlyInsight>('/ai/insights/monthly', { month, refresh: false }),
}
