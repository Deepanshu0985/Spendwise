import { apiClient } from './client'
import type { Goal } from './types'

export interface SaveGoalRequest {
  name: string
  targetAmount: number
  currentAmount?: number
  targetDate?: string
  currency?: string
}

export const goalsApi = {
  list: () => apiClient.get<Goal[]>('/goals'),
  create: (body: SaveGoalRequest) => apiClient.post<Goal>('/goals', body),
  update: (id: string, body: SaveGoalRequest) => apiClient.put<Goal>(`/goals/${id}`, body),
  remove: (id: string) => apiClient.delete<void>(`/goals/${id}`),
}
