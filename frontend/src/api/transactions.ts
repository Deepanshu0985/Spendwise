import { apiClient } from './client'
import type { PageMeta, Transaction, TransactionType } from './types'

export interface TransactionFilters {
  from?: string
  to?: string
  accountId?: string
  categoryId?: string
  merchantId?: string
  type?: string
  status?: string
  page?: number
  size?: number
  sort?: string
  [key: string]: string | number | undefined
}

export interface SplitInput {
  categoryId?: string
  amount: number
}

export interface CreateTransactionRequest {
  accountId: string
  merchantId?: string
  categoryId?: string
  transactionDate: string
  amount: number
  currency: string
  description?: string
  transactionType: TransactionType
  splits?: SplitInput[]
}

export interface UpdateTransactionRequest {
  accountId: string
  merchantId?: string
  categoryId?: string
  transactionDate: string
  amount: number
  description?: string
  status?: string
  splits?: SplitInput[]
}

export const transactionsApi = {
  list: (filters: TransactionFilters) =>
    apiClient.getWithMeta<Transaction[], PageMeta>('/transactions', filters),
  create: (body: CreateTransactionRequest, idempotencyKey?: string) =>
    apiClient.post<Transaction>('/transactions', body, idempotencyKey ? { 'Idempotency-Key': idempotencyKey } : undefined),
  update: (id: string, body: UpdateTransactionRequest) => apiClient.put<Transaction>(`/transactions/${id}`, body),
  remove: (id: string) => apiClient.delete<void>(`/transactions/${id}`),
}
