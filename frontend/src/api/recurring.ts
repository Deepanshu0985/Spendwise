import { apiClient } from './client'
import type { RecurringExpense } from './types'

export interface UpdateRecurringExpenseRequest {
  name?: string
  categoryId?: string
  confirmed?: boolean
}

export const recurringApi = {
  list: () => apiClient.get<RecurringExpense[]>('/recurring-expenses'),
  // Re-reads the user's confirmed expenses and returns the refreshed list.
  detect: () => apiClient.post<RecurringExpense[]>('/recurring-expenses/detect'),
  update: (id: string, body: UpdateRecurringExpenseRequest) => apiClient.put<RecurringExpense>(`/recurring-expenses/${id}`, body),
  dismiss: (id: string) => apiClient.post<void>(`/recurring-expenses/${id}/dismiss`),
}
