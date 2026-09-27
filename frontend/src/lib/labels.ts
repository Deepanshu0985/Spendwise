export const ACCOUNT_TYPE_LABELS: Record<string, string> = {
  BANK: 'Bank account',
  CREDIT_CARD: 'Credit card',
  CASH: 'Cash',
  WALLET: 'Wallet',
  OTHER: 'Other',
}

export const TRANSACTION_TYPE_LABELS: Record<string, string> = {
  EXPENSE: 'Expense',
  INCOME: 'Income',
  REFUND: 'Refund',
  FEE_CHARGED: 'Fee charged',
  INTEREST_CHARGED: 'Interest charged',
  INTEREST_EARNED: 'Interest earned',
  TRANSFER_OUT: 'Transfer out',
  TRANSFER_IN: 'Transfer in',
  CARD_PAYMENT_OUT: 'Card payment out',
  CARD_PAYMENT_IN: 'Card payment in',
  CASH_WITHDRAWAL: 'Cash withdrawal',
  UNKNOWN: 'Unresolved',
}

// The only types a user can assign directly when creating/editing a
// transaction - transfer and card-payment pairs only ever come from the
// dedicated transfer flow (TransactionServiceImpl.MANUALLY_ASSIGNABLE_TYPES).
export const MANUALLY_ASSIGNABLE_TYPES = [
  'EXPENSE',
  'INCOME',
  'REFUND',
  'FEE_CHARGED',
  'INTEREST_CHARGED',
  'INTEREST_EARNED',
  'CASH_WITHDRAWAL',
] as const

const DEBIT_TYPES = new Set(['EXPENSE', 'FEE_CHARGED', 'INTEREST_CHARGED', 'CASH_WITHDRAWAL', 'TRANSFER_OUT', 'CARD_PAYMENT_OUT'])

export function isDebit(transactionType: string): boolean {
  return DEBIT_TYPES.has(transactionType)
}

const PALETTE = ['#1D3557', '#1F4D3A', '#8C3B2E', '#6B4226', '#4A6741', '#A47148', '#5B4B8A', '#2E6E7E']

// A stable color per category/merchant name so badges stay visually
// consistent across renders without hardcoding a fixed category list.
export function colorForKey(key: string): string {
  let hash = 0
  for (let i = 0; i < key.length; i++) {
    hash = (hash * 31 + key.charCodeAt(i)) >>> 0
  }
  return PALETTE[hash % PALETTE.length]
}

export function initial(name: string): string {
  return name.trim().charAt(0).toUpperCase() || '?'
}
