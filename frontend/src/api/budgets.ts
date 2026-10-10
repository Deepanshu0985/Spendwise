import { apiClient } from './client'
import type { Budget, BudgetPeriodType } from './types'

export interface SaveBudgetRequest {
  name: string
  periodType: BudgetPeriodType
  startDate?: string
  endDate?: string
  totalLimit: number
  currency?: string
  categoryLimits: { categoryId: string; limitAmount: number }[]
}

export const budgetsApi = {
  list: () => apiClient.get<Budget[]>('/budgets'),
  create: (body: SaveBudgetRequest) => apiClient.post<Budget>('/budgets', body),
  update: (id: string, body: SaveBudgetRequest) => apiClient.put<Budget>(`/budgets/${id}`, body),
  remove: (id: string) => apiClient.delete<void>(`/budgets/${id}`),
}
