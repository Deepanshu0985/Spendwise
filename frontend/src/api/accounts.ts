import { apiClient } from './client'
import type { Account, AccountType } from './types'

export interface CreateAccountRequest {
  name: string
  accountType: AccountType
  institutionName?: string
  last4?: string
  currency: string
}

export interface UpdateAccountRequest {
  name: string
  institutionName?: string
  last4?: string
}

export const accountsApi = {
  list: () => apiClient.get<Account[]>('/accounts'),
  create: (body: CreateAccountRequest) => apiClient.post<Account>('/accounts', body),
  update: (id: string, body: UpdateAccountRequest) => apiClient.put<Account>(`/accounts/${id}`, body),
  deactivate: (id: string) => apiClient.delete<void>(`/accounts/${id}`),
}
