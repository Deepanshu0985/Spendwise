import { apiClient } from './client'
import type { ConfirmStatementResult, PageMeta, Statement, StatementTransaction, TransactionType } from './types'

export interface UpdateStagedTransactionRequest {
  transactionDate: string
  amount: number
  merchantId?: string
  categoryId?: string
  transactionType: TransactionType
}

export const statementsApi = {
  upload: (accountId: string, file: File) => {
    const form = new FormData()
    form.append('accountId', accountId)
    form.append('file', file)
    return apiClient.postForm<Statement>('/statements/upload', form)
  },
  list: (page = 0) => apiClient.getWithMeta<Statement[], PageMeta>('/statements', { page }),
  get: (id: string) => apiClient.get<Statement>(`/statements/${id}`),
  getTransactions: (id: string) => apiClient.get<StatementTransaction[]>(`/statements/${id}/transactions`),
  updateStagedTransaction: (statementId: string, stagingId: string, body: UpdateStagedTransactionRequest) =>
    apiClient.put<StatementTransaction>(`/statements/${statementId}/transactions/${stagingId}`, body),
  confirm: (id: string, idempotencyKey?: string) =>
    apiClient.post<ConfirmStatementResult>(`/statements/${id}/confirm`, undefined, idempotencyKey ? { 'Idempotency-Key': idempotencyKey } : undefined),
  retry: (id: string) => apiClient.post<Statement>(`/statements/${id}/retry`),
}
