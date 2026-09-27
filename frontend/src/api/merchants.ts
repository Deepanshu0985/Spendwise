import { apiClient } from './client'
import type { Merchant } from './types'

export const merchantsApi = {
  list: () => apiClient.get<Merchant[]>('/merchants'),
  create: (canonicalName: string) => apiClient.post<Merchant>('/merchants', { canonicalName }),
}
