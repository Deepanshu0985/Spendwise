import { useEffect, useMemo, useState } from 'react'
import { analyticsApi } from '../api/analytics'
import { transactionsApi } from '../api/transactions'
import type { CategoryBreakdownEntry, MerchantBreakdownEntry, Transaction, TrendPoint } from '../api/types'
import { CalendarIcon } from '../components/icons'
import { InsightCard } from '../components/InsightCard'
import { MoneyTip } from '../components/MoneyTip'
import { useReferenceData } from '../data/ReferenceDataContext'
import {
  currentMonthRange,
  formatCurrency,
  formatDate,
  formatDateRangeLabel,
  formatMonthLabel,
  formatSignedCurrency,
  lastNMonthsRange,
  previousMonthRange,
} from '../lib/format'
import { colorForKey, initial, isDebit } from '../lib/labels'
import { Link } from 'react-router-dom'

type Period = 'this-month' | 'last-month' | 'last-3-months'

const PERIODS: { key: Period; label: string; range: () => { from: string; to: string } }[] = [
  { key: 'this-month', label: 'This month', range: currentMonthRange },
  { key: 'last-month', label: 'Last month', range: previousMonthRange },
  { key: 'last-3-months', label: 'Last 3 months', range: () => lastNMonthsRange(3) },
]

interface DashboardData {
  currency: string
  expenses: number
  income: number
  savings: number
  savingsRate: number | null
  categories: CategoryBreakdownEntry[]
  merchants: MerchantBreakdownEntry[]
  excludedCurrencies: string[]
  excludedTransactionCount: number
}

export function DashboardPage() {
  const { categories: allCategories } = useReferenceData()
  const [period, setPeriod] = useState<Period>('this-month')
  const [data, setData] = useState<DashboardData | null>(null)
  const [trend, setTrend] = useState<TrendPoint[]>([])
  const [recent, setRecent] = useState<Transaction[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const range = useMemo(() => PERIODS.find((p) => p.key === period)!.range(), [period])
  const trendRange = useMemo(() => lastNMonthsRange(6), [])

  const categoryNameById = useMemo(() => new Map(allCategories.map((c) => [c.id, c.name])), [allCategories])

  useEffect(() => {
    let cancelled = false
    setLoading(true)
    setError(null)

    Promise.all([
      analyticsApi.monthly(range),
      analyticsApi.categories(range),
      analyticsApi.merchants(range),
      analyticsApi.trends(trendRange),
      transactionsApi.list({ from: range.from, to: range.to, size: 6, page: 0 }),
    ])
      .then(([monthly, categoryBreakdown, merchantBreakdown, trendSeries, transactions]) => {
        if (cancelled) return
        setData({
          currency: monthly.data.currency,
          expenses: monthly.data.expenses,
          income: monthly.data.income,
          savings: monthly.data.savings,
          savingsRate: monthly.data.savingsRate,
          categories: categoryBreakdown.data,
          merchants: merchantBreakdown.data,
          excludedCurrencies: monthly.meta.excludedCurrencies,
          excludedTransactionCount: monthly.meta.excludedTransactionCount,
        })
        setTrend(trendSeries.data)
        setRecent(transactions.data)
      })
      .catch(() => {
        if (!cancelled) setError('Could not load your dashboard. Please try again.')
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })

    return () => {
      cancelled = true
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [range.from, range.to, trendRange.from, trendRange.to])

  const maxCategoryAmount = Math.max(1, ...(data?.categories ?? []).map((c) => Math.abs(c.amount)))
  const maxTrendExpense = Math.max(1, ...trend.map((t) => t.expenses))
  const currency = data?.currency ?? 'INR'

  return (
    <div>
      <div className="page-header">
        <h1>Dashboard</h1>
        <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
          <div className="field-inline" style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            <CalendarIcon />
            {formatDateRangeLabel(range.from, range.to)}
          </div>
          <select
            className="field-inline"
            value={period}
            onChange={(e) => setPeriod(e.target.value as Period)}
            aria-label="Period"
          >
            {PERIODS.map((p) => (
              <option key={p.key} value={p.key}>
                {p.label}
              </option>
            ))}
          </select>
          <div className="field-inline" style={{ fontWeight: 600 }}>
            {currency}
          </div>
        </div>
      </div>

      <MoneyTip />

      {error && <div className="form-error-banner" style={{ marginBottom: 20 }}>{error}</div>}

      {loading && !data ? (
        <div className="loading-state" style={{ flexDirection: 'column' }}>
          <div><span className="spinner" /> Loading your dashboard&hellip;</div>
          <MoneyTip variant="inline" />
        </div>
      ) : data ? (
        <>
          {data.excludedTransactionCount > 0 && (
            <div className="form-error-banner" style={{ marginBottom: 20, background: '#F1E6D6', color: '#6B4226' }}>
              {data.excludedTransactionCount} transaction{data.excludedTransactionCount === 1 ? '' : 's'} in{' '}
              {data.excludedCurrencies.join(', ')} {data.excludedTransactionCount === 1 ? 'was' : 'were'} left out of
              these figures (only {currency} counts here).
            </div>
          )}

          <div className="summary-grid">
            <div className="summary-card">
              <div className="summary-label">Income</div>
              <div className="summary-value">{formatCurrency(data.income, currency)}</div>
              <div className="summary-note positive">for this period</div>
            </div>
            <div className="summary-card">
              <div className="summary-label">Expenses</div>
              <div className="summary-value">{formatCurrency(data.expenses, currency)}</div>
              <div className="summary-note negative">across {data.categories.length} categor{data.categories.length === 1 ? 'y' : 'ies'}</div>
            </div>
            <div className="summary-card">
              <div className="summary-label">Savings</div>
              <div className="summary-value">{formatCurrency(data.savings, currency)}</div>
              <div className="summary-note">income &minus; expenses</div>
            </div>
            <div className="summary-card">
              <div className="summary-label">Savings rate</div>
              <div className="summary-value">{data.savingsRate === null ? '—' : `${data.savingsRate.toFixed(1)}%`}</div>
              <div className="summary-note">of every 100 earned</div>
            </div>
          </div>

          <InsightCard month={period === 'last-3-months' ? null : range.from.slice(0, 7)} />

          <div className="dashboard-grid">
            <div className="card">
              <div className="card-title">Spending by category</div>
              {data.categories.length === 0 ? (
                <div className="empty-state">No expenses in this period yet.</div>
              ) : (
                [...data.categories]
                  .sort((a, b) => Math.abs(b.amount) - Math.abs(a.amount))
                  .map((entry) => (
                    <div className="bar-row" key={entry.categoryId ?? 'uncategorized'}>
                      <div className="bar-row-labels">
                        <span className="name">{entry.categoryName}</span>
                        <span className="amount">{formatCurrency(entry.amount, currency)}</span>
                      </div>
                      <div className="bar-track">
                        <div
                          className="bar-fill"
                          style={{
                            width: `${Math.max(2, (Math.abs(entry.amount) / maxCategoryAmount) * 100)}%`,
                            background: colorForKey(entry.categoryName),
                          }}
                        />
                      </div>
                    </div>
                  ))
              )}
            </div>

            <div className="card">
              <div className="card-title">Expenses trend</div>
              {trend.length === 0 ? (
                <div className="empty-state">Not enough data yet.</div>
              ) : (
                <div className="trend-chart">
                  {trend.map((point, i) => {
                    const isCurrent = i === trend.length - 1
                    const height = Math.max(4, (point.expenses / maxTrendExpense) * 100)
                    return (
                      <div className="trend-bar-col" key={point.month}>
                        <div className={`trend-bar${isCurrent ? ' current' : ''}`} style={{ height: `${height}%` }} />
                        <div className={`trend-label${isCurrent ? ' current' : ''}`}>{formatMonthLabel(point.month)}</div>
                      </div>
                    )
                  })}
                </div>
              )}
            </div>
          </div>

          <div className="dashboard-grid-secondary">
            <div className="card">
              <div className="card-title">Top merchants</div>
              {data.merchants.length === 0 ? (
                <div className="empty-state">No merchant spending yet.</div>
              ) : (
                [...data.merchants]
                  .sort((a, b) => Math.abs(b.amount) - Math.abs(a.amount))
                  .slice(0, 5)
                  .map((entry) => (
                    <div className="list-row" key={entry.merchantId ?? 'unknown'}>
                      <div className="list-row-name">
                        <div className="list-avatar">{initial(entry.merchantName)}</div>
                        <span>{entry.merchantName}</span>
                      </div>
                      <span style={{ fontWeight: 600 }}>{formatCurrency(entry.amount, currency)}</span>
                    </div>
                  ))
              )}
            </div>

            <div className="card">
              <div className="card-title" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <span>Recent transactions</span>
                <Link to="/transactions" style={{ fontSize: 13, fontWeight: 600 }}>
                  View all &rarr;
                </Link>
              </div>
              {recent.length === 0 ? (
                <div className="empty-state">No transactions logged yet.</div>
              ) : (
                <div className="table-wrap">
                  <table>
                    <thead>
                      <tr>
                        <th>Date</th>
                        <th>Description</th>
                        <th>Category</th>
                        <th className="align-right">Amount</th>
                      </tr>
                    </thead>
                    <tbody>
                      {recent.map((tx) => {
                        const negative = isDebit(tx.transactionType)
                        const categoryName = tx.categoryId ? categoryNameById.get(tx.categoryId) : null
                        return (
                          <tr key={tx.id}>
                            <td>{formatDate(tx.transactionDate)}</td>
                            <td>{tx.description || <span style={{ color: 'var(--ink-faint)' }}>No description</span>}</td>
                            <td>{categoryName ?? '—'}</td>
                            <td className={`align-right ${negative ? 'amount-negative' : 'amount-positive'}`}>
                              {formatSignedCurrency(tx.amount, tx.currency, negative)}
                            </td>
                          </tr>
                        )
                      })}
                    </tbody>
                  </table>
                </div>
              )}
            </div>
          </div>
        </>
      ) : null}
    </div>
  )
}
