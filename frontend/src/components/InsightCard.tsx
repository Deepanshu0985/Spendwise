import { useEffect, useState } from 'react'
import { ApiRequestError } from '../api/client'
import { insightsApi } from '../api/insights'
import type { MonthlyInsight } from '../api/types'
import { InsightDashboard } from './insight/InsightDashboard'
import { Button } from './ui/Button'

interface Props {
  /** yyyy-mm, or null when the chosen period is not a single calendar month */
  month: string | null
}

/**
 * The month's insight as a visual dashboard. It is only fetched when it already exists; writing a new one is the user's click, so
 * opening the dashboard never calls the AI. The figures were computed by the application and any wording was checked before it was stored.
 */
export function InsightCard({ month }: Props) {
  const [insight, setInsight] = useState<MonthlyInsight | null>(null)
  const [loading, setLoading] = useState(false)
  const [working, setWorking] = useState(false)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let cancelled = false
    setInsight(null)
    setError(null)
    if (!month) return
    setLoading(true)
    insightsApi
      .get(month)
      .then((found) => {
        if (!cancelled) setInsight(found)
      })
      .catch(() => {
        // the card is optional: failing to look should not disturb the dashboard
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })
    return () => {
      cancelled = true
    }
  }, [month])

  async function write() {
    if (!month) return
    setError(null)
    setWorking(true)
    try {
      setInsight(await insightsApi.generate(month))
    } catch (err) {
      setError(err instanceof ApiRequestError ? err.message : 'Something went wrong. Please try again.')
    } finally {
      setWorking(false)
    }
  }

  return (
    <div className="card insight-card">
      <div className="insight-head">
        <div>
          <div className="card-title" style={{ marginBottom: 0 }}>Insight</div>
          {insight && <div className="insight-title">{insight.title}</div>}
        </div>
        {month && (
          <Button variant="secondary" small onClick={() => void write()} disabled={working || loading}>
            {working ? 'Working…' : insight ? 'Update' : 'Explain this month'}
          </Button>
        )}
      </div>

      {!month && <div className="empty-state">Choose “This month” or “Last month” to see an insight for that month.</div>}
      {month && loading && <div className="insight-note">Checking for an insight…</div>}
      {month && !loading && !insight && !working && !error && (
        <div className="insight-note">No insight yet. It turns the month into charts and a few sentences, using only your own figures.</div>
      )}
      {error && <div className="form-error-banner" style={{ marginTop: 10 }}>{error}</div>}

      {insight && (
        // keyed by when it was written, so an updated insight replays its entrance animation
        <div style={{ marginTop: 14 }} key={`${insight.month}-${insight.generatedAt}`}>
          <InsightDashboard insight={insight} />
          <div className="insight-note">
            {insight.writtenByAi ? `Words written by AI (${insight.modelName}); every figure computed by Spendwise` : 'Written from your figures'}
            {insight.generatedAt ? ` · ${new Date(insight.generatedAt).toLocaleDateString('en-IN')}` : ''}
          </div>
          {insight.note && <div className="insight-note">{insight.note}</div>}
        </div>
      )}
    </div>
  )
}
