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
  upload: (accountId: string, file: File, password?: string) => {
    const form = new FormData()
    form.append('accountId', accountId)
    form.append('file', file)
    if (password) form.append('password', password)
    return apiClient.postForm<Statement>('/statements/upload', form)
  },
  list: (page = 0) => apiClient.getWithMeta<Statement[], PageMeta>('/statements', { page }),
  get: (id: string) => apiClient.get<Statement>(`/statements/${id}`),
  getTransactions: (id: string) => apiClient.get<StatementTransaction[]>(`/statements/${id}/transactions`),
  updateStagedTransaction: (statementId: string, stagingId: string, body: UpdateStagedTransactionRequest) =>
    apiClient.put<StatementTransaction>(`/statements/${statementId}/transactions/${stagingId}`, body),
  confirm: (id: string, idempotencyKey?: string) =>
    apiClient.post<ConfirmStatementResult>(`/statements/${id}/confirm`, undefined, idempotencyKey ? { 'Idempotency-Key': idempotencyKey } : undefined),
  recheckDuplicates: (id: string) => apiClient.post<StatementTransaction[]>(`/statements/${id}/duplicates/recheck`),
  keepDuplicate: (statementId: string, stagingId: string) =>
    apiClient.post<StatementTransaction>(`/statements/${statementId}/transactions/${stagingId}/keep-duplicate`),
  skipRow: (statementId: string, stagingId: string) =>
    apiClient.post<StatementTransaction>(`/statements/${statementId}/transactions/${stagingId}/skip`),
  restoreRow: (statementId: string, stagingId: string) =>
    apiClient.post<StatementTransaction>(`/statements/${statementId}/transactions/${stagingId}/restore`),
  mapSourceAccount: (statementId: string, label: string, accountId: string) =>
    apiClient.put<StatementTransaction[]>(`/statements/${statementId}/source-accounts`, { label, accountId }),
  retry: (id: string, password?: string) =>
    apiClient.post<Statement>(`/statements/${id}/retry`, password ? { password } : undefined),
  supportedBanks: () => apiClient.get<string[]>('/statements/supported-banks'),
}
