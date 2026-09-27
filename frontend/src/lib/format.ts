// Locale-aware display formatting only (design-system.md's Financial Formatting
// principle) - backend precision (NUMERIC(19,4)) is never touched, and none of
// this feeds back into a calculation.

export function formatCurrency(amount: number, currency: string): string {
  return new Intl.NumberFormat('en-IN', { style: 'currency', currency, maximumFractionDigits: 2 }).format(amount)
}

export function formatSignedCurrency(amount: number, currency: string, negative: boolean): string {
  const formatted = formatCurrency(Math.abs(amount), currency)
  return negative ? `−${formatted}` : `+${formatted}`
}

export function formatDate(isoDate: string): string {
  const [year, month, day] = isoDate.split('-').map(Number)
  const d = new Date(year, month - 1, day)
  return d.toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' })
}

const MONTH_NAMES = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec']

export function formatMonthLabel(yearMonth: string): string {
  const [, month] = yearMonth.split('-')
  return MONTH_NAMES[Number(month) - 1] ?? yearMonth
}

// Local calendar components only - never toISOString(), which converts to UTC
// and can shift the date by a day depending on the browser's timezone offset
// (analytics-specification.md's Period Semantics: no timezone conversion is
// ever applied to a transaction_date).
export function toIsoDate(d: Date): string {
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

export function today(): string {
  return toIsoDate(new Date())
}

export function currentMonthRange(): { from: string; to: string } {
  const now = new Date()
  const from = new Date(now.getFullYear(), now.getMonth(), 1)
  const to = new Date(now.getFullYear(), now.getMonth() + 1, 0)
  return { from: toIsoDate(from), to: toIsoDate(to) }
}

export function previousMonthRange(): { from: string; to: string } {
  const now = new Date()
  const from = new Date(now.getFullYear(), now.getMonth() - 1, 1)
  const to = new Date(now.getFullYear(), now.getMonth(), 0)
  return { from: toIsoDate(from), to: toIsoDate(to) }
}

export function lastNMonthsRange(n: number): { from: string; to: string } {
  const now = new Date()
  const from = new Date(now.getFullYear(), now.getMonth() - (n - 1), 1)
  const to = new Date(now.getFullYear(), now.getMonth() + 1, 0)
  return { from: toIsoDate(from), to: toIsoDate(to) }
}

export function formatDateRangeLabel(from: string, to: string): string {
  return `${formatDate(from)} – ${formatDate(to)}`
}
