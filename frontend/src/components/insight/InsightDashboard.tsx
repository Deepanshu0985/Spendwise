import type { CSSProperties } from 'react'
import type { InsightMetrics, InsightUnusual, MonthlyInsight } from '../../api/types'
import { formatCurrency } from '../../lib/format'
import { useCountUp, useReveal } from '../../lib/motion'

const MONTH_NAMES = ['January', 'February', 'March', 'April', 'May', 'June', 'July', 'August', 'September', 'October', 'November', 'December']

function monthLabel(yearMonth: string): string {
  const [year, month] = yearMonth.split('-').map(Number)
  return `${MONTH_NAMES[month - 1] ?? yearMonth} ${year}`
}

function Money({ value, currency }: { value: number; currency: string }) {
  return <>{formatCurrency(useCountUp(value), currency)}</>
}

function Times({ value }: { value: number }) {
  return <>{useCountUp(value).toFixed(1)}×</>
}

const STATUS_CLASS = { ON_TRACK: 'ok', CLOSE_TO_LIMIT: 'warn', OVER_BUDGET: 'over' } as const

/** A bar that grows from zero to its share when the section appears; delay staggers a group of them. */
function Bar({ percent, tone, shown, delay = 0 }: { percent: number; tone: string; shown: boolean; delay?: number }) {
  const width = shown ? Math.max(0, Math.min(100, percent)) : 0
  const style: CSSProperties = { width: `${width}%`, transitionDelay: `${delay}ms` }
  return (
    <div className="ins-track">
      <div className={`ins-fill ${tone}`} style={style} />
    </div>
  )
}

function SavingsRing({ rate, savings, shown, currency }: { rate: number | null; savings: number; shown: boolean; currency: string }) {
  const radius = 48
  const circumference = 2 * Math.PI * radius
  const percent = rate === null ? 0 : Math.max(0, Math.min(100, rate))
  const counted = useCountUp(rate ?? 0)
  return (
    <div className="ins-ring-wrap">
      <svg viewBox="0 0 120 120" className="ins-ring" role="img" aria-label={rate === null ? 'No savings rate' : `Savings rate ${rate.toFixed(1)} percent`}>
        <circle className="ins-ring-bg" cx="60" cy="60" r={radius} />
        <circle
          className={`ins-ring-fg${savings < 0 ? ' negative' : ''}`}
          cx="60"
          cy="60"
          r={radius}
          strokeDasharray={circumference}
          strokeDashoffset={shown ? circumference * (1 - percent / 100) : circumference}
          transform="rotate(-90 60 60)"
        />
      </svg>
      <div className="ins-ring-center">
        <div className="ins-ring-value">{rate === null ? '—' : `${counted.toFixed(1)}%`}</div>
        <div className="ins-ring-label">saved</div>
      </div>
      <div className="ins-ring-caption">
        {savings >= 0 ? (
          <>
            <Money value={savings} currency={currency} /> kept
          </>
        ) : (
          <>
            <Money value={Math.abs(savings)} currency={currency} /> overspent
          </>
        )}
      </div>
    </div>
  )
}

function ChangeChip({ metrics }: { metrics: InsightMetrics }) {
  if (!metrics.previousMonth || !metrics.expensesChange) return null
  const { difference, percent } = metrics.expensesChange
  const previous = monthLabel(metrics.previousMonth.month)
  if (difference === 0) return <div className="ins-chip flat">Spending the same as {previous}</div>
  const up = difference > 0
  return (
    <div className={`ins-chip ${up ? 'up' : 'down'}`}>
      {up ? '▲' : '▼'} {formatCurrency(Math.abs(difference), metrics.currency)}
      {percent !== null ? ` (${Math.abs(percent).toFixed(1)}%)` : ''} {up ? 'more' : 'less'} spent than {previous}
    </div>
  )
}

function Comparison({ metrics, shown }: { metrics: InsightMetrics; shown: boolean }) {
  const previous = metrics.previousMonth
  const values = [metrics.income, metrics.expenses, Math.abs(metrics.savings), previous?.income ?? 0, previous?.expenses ?? 0, Math.abs(previous?.savings ?? 0)]
  const scale = Math.max(1, ...values)
  const rows = [
    { key: 'income', label: 'Income', tone: 'income', now: metrics.income, before: previous?.income },
    { key: 'spent', label: 'Spent', tone: 'spent', now: metrics.expenses, before: previous?.expenses },
    { key: 'saved', label: 'Saved', tone: 'saved', now: metrics.savings, before: previous?.savings },
  ]
  return (
    <div className="ins-compare">
      {rows.map((row, index) => (
        <div className="ins-compare-row" key={row.key}>
          <div className="ins-compare-head">
            <span>{row.label}</span>
            <strong>
              <Money value={row.now} currency={metrics.currency} />
            </strong>
          </div>
          <Bar percent={(Math.max(0, row.now) / scale) * 100} tone={row.tone} shown={shown} delay={index * 120} />
          {previous && row.before !== undefined && (
            <>
              <Bar percent={(Math.max(0, row.before) / scale) * 100} tone={`${row.tone} ghost`} shown={shown} delay={index * 120 + 80} />
              <div className="ins-compare-prev">
                {monthLabel(previous.month).split(' ')[0]}: {formatCurrency(row.before, metrics.currency)}
              </div>
            </>
          )}
        </div>
      ))}
      {!previous && <div className="ins-compare-prev">No earlier month to compare with yet.</div>}
    </div>
  )
}

function Unusual({ item, currency, shown, index }: { item: InsightUnusual; currency: string; shown: boolean; index: number }) {
  const usualShare = item.amount > 0 ? (item.typical / item.amount) * 100 : 0
  return (
    <div className="ins-flag" style={{ animationDelay: `${index * 120}ms` }}>
      <div className="ins-flag-top">
        <span className="ins-flag-icon" aria-hidden="true">⚑</span>
        <div className="ins-flag-label">
          {item.kind === 'LARGE_PAYMENT' ? `Payment: ${item.label}` : item.label}
          {item.date ? <span className="ins-flag-date"> · {new Date(item.date + 'T00:00:00').toLocaleDateString('en-IN', { day: 'numeric', month: 'short' })}</span> : null}
        </div>
        <div className="ins-flag-times">
          <Times value={item.timesTypical} />
        </div>
      </div>
      <Bar percent={100} tone="spent" shown={shown} delay={index * 120} />
      <div className="ins-flag-line">
        <span>This month</span>
        <strong>{formatCurrency(item.amount, currency)}</strong>
      </div>
      <Bar percent={usualShare} tone="spent ghost" shown={shown} delay={index * 120 + 100} />
      <div className="ins-flag-line">
        <span>{item.kind === 'LARGE_PAYMENT' ? 'A usual payment' : 'Usually'}</span>
        <strong>{formatCurrency(item.typical, currency)}</strong>
      </div>
    </div>
  )
}

/**
 * The month drawn as charts from the figures the server computed: a savings-rate ring, this month against last month, where the money
 * went, anything unusual, and budgets. The checked written summary sits underneath. Bars grow and numbers count up when it appears
 * (and not at all when the person prefers reduced motion).
 */
export function InsightDashboard({ insight }: { insight: MonthlyInsight }) {
  const shown = useReveal()
  const m = insight.metrics
  const hasActivity = m.income !== 0 || m.expenses !== 0
  const maxCategory = Math.max(1, ...m.topCategories.map((c) => c.amount))

  if (!hasActivity) {
    return (
      <div className="ins-empty">
        <div className="ins-empty-title">{monthLabel(m.month)}</div>
        <div>Nothing was recorded this month, so there is nothing to chart yet.</div>
      </div>
    )
  }

  return (
    <div className="ins-root">
      <div className="ins-row ins-top">
        <div className="ins-panel ins-fade" style={{ animationDelay: '0ms' }}>
          <div className="ins-panel-title">Savings rate</div>
          <SavingsRing rate={m.savingsRate} savings={m.savings} shown={shown} currency={m.currency} />
        </div>
        <div className="ins-panel ins-fade ins-wide" style={{ animationDelay: '90ms' }}>
          <div className="ins-panel-title">
            {monthLabel(m.month)}
            {m.previousMonth ? <span className="ins-legend"><i className="solid" /> this month <i className="ghost" /> {monthLabel(m.previousMonth.month).split(' ')[0]}</span> : null}
          </div>
          <Comparison metrics={m} shown={shown} />
          <ChangeChip metrics={m} />
        </div>
      </div>

      <div className="ins-row">
        <div className="ins-panel ins-fade ins-wide" style={{ animationDelay: '180ms' }}>
          <div className="ins-panel-title">Where it went</div>
          {m.topCategories.length === 0 ? (
            <div className="ins-muted">No spending by category yet.</div>
          ) : (
            m.topCategories.map((category, index) => (
              <div className="ins-cat" key={category.name}>
                <div className="ins-cat-head">
                  <span>{category.name}</span>
                  <span>
                    <strong>{formatCurrency(category.amount, m.currency)}</strong>
                    {category.sharePercent !== undefined ? <em> · {category.sharePercent.toFixed(1)}%</em> : null}
                  </span>
                </div>
                <Bar percent={(category.amount / maxCategory) * 100} tone={`cat-${index % 3}`} shown={shown} delay={200 + index * 120} />
              </div>
            ))
          )}
          {m.topMerchants.length > 0 && (
            <div className="ins-merchants">
              {m.topMerchants.map((merchant) => (
                <span className="ins-merchant" key={merchant.name}>
                  {merchant.name} · {formatCurrency(merchant.amount, m.currency)}
                </span>
              ))}
            </div>
          )}
        </div>

        {m.budgets.length > 0 && (
          <div className="ins-panel ins-fade" style={{ animationDelay: '270ms' }}>
            <div className="ins-panel-title">Budgets</div>
            {m.budgets.map((budget, index) => (
              <div className="ins-budget" key={budget.name}>
                <div className="ins-cat-head">
                  <span>{budget.name}</span>
                  <strong>{budget.percentUsed.toFixed(0)}%</strong>
                </div>
                <Bar percent={budget.percentUsed} tone={`budget-${STATUS_CLASS[budget.status]}`} shown={shown} delay={300 + index * 120} />
                <div className="ins-muted">
                  {formatCurrency(budget.spent, m.currency)} of {formatCurrency(budget.limit, m.currency)}
                  {budget.overBy !== undefined
                    ? ` · ${formatCurrency(budget.overBy, m.currency)} over`
                    : budget.remaining !== undefined
                      ? ` · ${formatCurrency(budget.remaining, m.currency)} left`
                      : ''}
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {m.unusual.length > 0 && (
        <div className="ins-panel ins-fade" style={{ animationDelay: '360ms' }}>
          <div className="ins-panel-title">Worth a look</div>
          <div className="ins-flags">
            {m.unusual.map((item, index) => (
              <Unusual key={`${item.kind}-${item.label}-${index}`} item={item} currency={m.currency} shown={shown} index={index} />
            ))}
          </div>
        </div>
      )}

      <div className="ins-words ins-fade" style={{ animationDelay: '450ms' }}>
        <div className="ins-panel-title">In words</div>
        <p>{insight.summary}</p>
        {insight.highlights.length > 0 && (
          <ul>
            {insight.highlights.map((highlight, i) => (
              <li key={i}>{highlight}</li>
            ))}
          </ul>
        )}
      </div>
    </div>
  )
}
