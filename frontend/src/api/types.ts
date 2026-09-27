// Mirrors the backend's response DTOs (infrastructure.web.*) field for field.
// One place for these shapes so pages never guess at JSON keys.

export interface UserProfile {
  id: string
  email: string
  fullName: string
  defaultCurrency: string
  timezone: string
}

export type AccountType = 'BANK' | 'CREDIT_CARD' | 'CASH' | 'WALLET' | 'OTHER'

export interface Account {
  id: string
  name: string
  accountType: AccountType
  institutionName: string | null
  last4: string | null
  currency: string
  active: boolean
}

export type CategoryType = 'EXPENSE' | 'INCOME'

export interface Category {
  id: string
  name: string
  categoryType: CategoryType
  parentId: string | null
  system: boolean
  active: boolean
}

export interface Merchant {
  id: string
  canonicalName: string
  normalizedKey: string
}

export type TransferKind = 'TRANSFER' | 'CARD_PAYMENT'

export type TransactionType =
  | 'EXPENSE'
  | 'INCOME'
  | 'REFUND'
  | 'FEE_CHARGED'
  | 'INTEREST_CHARGED'
  | 'INTEREST_EARNED'
  | 'TRANSFER_OUT'
  | 'TRANSFER_IN'
  | 'CARD_PAYMENT_OUT'
  | 'CARD_PAYMENT_IN'
  | 'CASH_WITHDRAWAL'
  | 'UNKNOWN'

export type TransactionStatus = 'PENDING' | 'CONFIRMED' | 'IGNORED' | 'DELETED'

export interface TransactionSplit {
  categoryId: string | null
  amount: number
}

export interface Transaction {
  id: string
  accountId: string
  merchantId: string | null
  categoryId: string | null
  transactionDate: string
  amount: number
  currency: string
  description: string | null
  transactionType: TransactionType
  source: 'MANUAL' | 'STATEMENT' | 'IMPORT' | 'SYSTEM'
  status: TransactionStatus
  transferGroupId: string | null
  splits: TransactionSplit[]
}

export interface PageMeta {
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface MonthlySummary {
  currency: string
  expenses: number
  income: number
  savings: number
  savingsRate: number | null
}

export interface CategoryBreakdownEntry {
  categoryId: string | null
  categoryName: string
  amount: number
}

export interface MerchantBreakdownEntry {
  merchantId: string | null
  merchantName: string
  amount: number
}

export interface TrendPoint {
  month: string
  expenses: number
  income: number
  savings: number
  savingsRate: number | null
}

export interface AnalyticsMeta {
  currency: string
  excludedCurrencies: string[]
  excludedTransactionCount: number
}
