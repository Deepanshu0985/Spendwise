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

export type StatementStatus = 'UPLOADED' | 'PROCESSING' | 'READY_FOR_REVIEW' | 'IMPORTED' | 'FAILED'

export interface Statement {
  id: string
  accountId: string
  fileName: string
  fileType: string
  periodStart: string | null
  periodEnd: string | null
  status: StatementStatus
  errorMessage: string | null
  createdAt: string
  processedAt: string | null
}

export type DuplicateStatus = 'UNKNOWN' | 'NOT_DUPLICATE' | 'POSSIBLE_DUPLICATE' | 'DUPLICATE'
export type DuplicateReason = 'EXACT_REFERENCE' | 'DATE_AMOUNT_DESCRIPTION' | 'NEARBY_SIMILAR' | 'MANUAL_ENTRY_MATCH'
export type ReviewStatus = 'PENDING' | 'ACCEPTED' | 'EDITED' | 'REJECTED'

export interface StatementTransaction {
  id: string
  statementId: string
  transactionDate: string
  amount: number
  currency: string
  rawDescription: string
  normalizedDescription: string | null
  suggestedMerchantId: string | null
  suggestedCategoryId: string | null
  suggestedTransactionType: TransactionType
  confidenceScore: number
  duplicateStatus: DuplicateStatus
  reviewStatus: ReviewStatus
  canonicalTransactionId: string | null
  duplicateReason: DuplicateReason | null
  duplicateOfTransactionId: string | null
  duplicateOverridden: boolean
  sourceAccountLabel: string | null
  accountId: string | null
  aiSuggested: boolean
  aiReason: string | null
}

export interface SuggestCategoriesResult {
  rows: StatementTransaction[]
  ruleApplied: number
  aiApplied: number
  needsReview: number
  notAsked: number
  aiAvailable: boolean
  stoppedReason: 'UNAVAILABLE' | 'LIMIT' | null
  limitMessage: string | null
}

export interface ConfirmStatementResult {
  statement: Statement
  importedTransactionIds: string[]
  skippedDuplicateCount: number
}

export type RecurrenceFrequency = 'WEEKLY' | 'MONTHLY' | 'QUARTERLY' | 'YEARLY'

export interface RecurringExpense {
  id: string
  name: string
  merchantId: string | null
  categoryId: string | null
  currency: string
  averageAmount: number
  frequency: RecurrenceFrequency
  lastSeenDate: string
  nextExpectedDate: string
  monthlyEstimate: number
  yearlyEstimate: number
  occurrences: number
  confidenceScore: number
  isActive: boolean
  confirmed: boolean
}

export type BudgetPeriodType = 'MONTHLY' | 'CUSTOM'
export type BudgetStatus = 'ON_TRACK' | 'CLOSE_TO_LIMIT' | 'OVER_BUDGET'

export interface BudgetCategoryProgress {
  categoryId: string
  limitAmount: number
  spent: number
  remaining: number
  percentUsed: number
  status: BudgetStatus
}

export interface Budget {
  id: string
  name: string
  periodType: BudgetPeriodType
  startDate: string
  endDate: string | null
  totalLimit: number
  currency: string
  progress: {
    windowStart: string
    windowEnd: string
    totalSpent: number
    remaining: number
    percentUsed: number
    status: BudgetStatus
    categories: BudgetCategoryProgress[]
  }
}

export type GoalStatus = 'ACTIVE' | 'ACHIEVED'

export interface Goal {
  id: string
  name: string
  targetAmount: number
  currentAmount: number
  targetDate: string | null
  currency: string
  status: GoalStatus
  remaining: number
  percentComplete: number
  requiredPerMonth: number | null
  overdue: boolean
}

export interface AssistantTurn {
  role: 'user' | 'assistant'
  content: string
}

export interface AssistantSource {
  date: string
  description: string
  merchant: string | null
  amount: number
  currency: string
  type: string
}

export interface AssistantReply {
  answer: string
  toolsUsed: { name: string; context: string }[]
  sources: AssistantSource[]
  fallback: boolean
  remainingMessagesToday: number
}

export interface AiStatus {
  enabled: boolean
  remainingRowsToday: number
  dailyLimit: number
  remainingMessagesToday: number
  dailyMessageLimit: number
}

export interface InsightUnusual {
  kind: 'CATEGORY_SPIKE' | 'LARGE_PAYMENT'
  label: string
  amount: number
  typical: number
  increase: number
  timesTypical: number
  date?: string
}

/** The figures an insight was written from (computed by the server); the charts are drawn from these. */
export interface InsightMetrics {
  month: string
  currency: string
  income: number
  expenses: number
  savings: number
  savingsRate: number | null
  previousMonth?: { month: string; income: number; expenses: number; savings: number }
  expensesChange?: { difference: number; percent: number | null }
  topCategories: { name: string; amount: number; sharePercent?: number }[]
  topMerchants: { name: string; amount: number }[]
  unusual: InsightUnusual[]
  incomeChange?: { difference: number; percent: number | null }
  savingsChange?: { difference: number }
  budgets: {
    name: string
    limit: number
    spent: number
    percentUsed: number
    status: 'ON_TRACK' | 'CLOSE_TO_LIMIT' | 'OVER_BUDGET'
    remaining?: number
    overBy?: number
    percentOver?: number
  }[]
}

export interface MonthlyInsight {
  month: string
  title: string
  summary: string
  highlights: string[]
  metrics: InsightMetrics
  writtenByAi: boolean
  modelName: string
  promptVersion: string
  generatedAt: string | null
  cached: boolean
  note: string | null
}
