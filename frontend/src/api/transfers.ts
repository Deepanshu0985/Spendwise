import { apiClient } from './client'
import type { Transaction, TransferKind } from './types'

export interface CreateTransferRequest {
  fromAccountId: string
  toAccountId: string
  transactionDate: string
  amount: number
  currency: string
  kind: TransferKind
}

export interface TransferResult {
  outTransaction: Transaction
  inTransaction: Transaction
}

export const transferApi = {
  create: (body: CreateTransferRequest) => apiClient.post<TransferResult>('/transactions/transfer', body),
}
