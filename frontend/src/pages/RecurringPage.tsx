import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { ApiRequestError } from '../api/client'
import { recurringApi } from '../api/recurring'
import type { RecurrenceFrequency, RecurringExpense } from '../api/types'
import { EditIcon } from '../components/icons'
import { Button } from '../components/ui/Button'
import { Field } from '../components/ui/Field'
import { Modal } from '../components/ui/Modal'
import { formatCurrency, formatDate, today } from '../lib/format'

const FREQUENCY_LABELS: Record<RecurrenceFrequency, string> = {
  WEEKLY: 'Weekly',
  MONTHLY: 'Monthly',
  QUARTERLY: 'Every 3 months',
  YEARLY: 'Yearly',
}

const linkButton = { background: 'none', border: 'none', padding: 0, cursor: 'pointer', color: 'var(--accent)', fontSize: 13 } as const

export function RecurringPage() {
  const [items, setItems] = useState<RecurringExpense[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [editing, setEditing] = useState<RecurringExpense | null>(null)

  async function load() {
    setLoading(true)
    setError(null)
    try {
      // Detect on open: it is cheap, and picks up anything confirmed since the last visit.
      setItems(await recurringApi.detect())
    } catch (err) {
      setError(err instanceof ApiRequestError ? err.message : 'Something went wrong. Please try again.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void load()
  }, [])

  async function handleConfirm(item: RecurringExpense) {
    setError(null)
    try {
      const updated = await recurringApi.update(item.id, { confirmed: !item.confirmed })
      setItems((current) => current.map((i) => (i.id === updated.id ? updated : i)))
    } catch (err) {
      setError(err instanceof ApiRequestError ? err.message : 'Something went wrong. Please try again.')
    }
  }

  async function handleDismiss(item: RecurringExpense) {
    setError(null)
    try {
      await recurringApi.dismiss(item.id)
      setItems((current) => current.filter((i) => i.id !== item.id))
    } catch (err) {
      setError(err instanceof ApiRequestError ? err.message : 'Something went wrong. Please try again.')
    }
  }

  const active = items.filter((i) => i.isActive)
  const stopped = items.filter((i) => !i.isActive)
  // Totals are per currency: adding rupees to dollars would be meaningless.
  const totals = new Map<string, { monthly: number; yearly: number }>()
  active.forEach((i) => {
    const t = totals.get(i.currency) ?? { monthly: 0, yearly: 0 }
    t.monthly += i.monthlyEstimate
    t.yearly += i.yearlyEstimate
    totals.set(i.currency, t)
  })
  const money = (pick: 'monthly' | 'yearly') =>
    totals.size === 0 ? '—' : [...totals.entries()].map(([currency, t]) => formatCurrency(t[pick], currency)).join(' + ')

  function dueNote(item: RecurringExpense): string {
    if (!item.isActive) return 'Seems to have stopped'
    return item.nextExpectedDate < today() ? `Was expected ${formatDate(item.nextExpectedDate)}` : formatDate(item.nextExpectedDate)
  }

  function renderRows(list: RecurringExpense[]) {
    return list.map((item) => (
      <tr key={item.id} style={item.isActive ? undefined : { opacity: 0.6 }}>
        <td style={{ paddingLeft: 22 }}>
          {item.name}
          <div className="page-subtitle" style={{ marginTop: 2 }}>
            seen {item.occurrences} times · last {formatDate(item.lastSeenDate)}
          </div>
        </td>
        <td>{FREQUENCY_LABELS[item.frequency]}</td>
        <td className="align-right">{formatCurrency(item.averageAmount, item.currency)}</td>
        <td className="align-right">{formatCurrency(item.monthlyEstimate, item.currency)}</td>
        <td>{dueNote(item)}</td>
        <td style={{ paddingRight: 22 }}>
          <div style={{ display: 'flex', gap: '6px 14px', alignItems: 'center', flexWrap: 'wrap' }}>
            {item.confirmed ? <span className="badge positive">Confirmed</span> : null}
            <button type="button" style={linkButton} onClick={() => void handleConfirm(item)}>
              {item.confirmed ? 'Unconfirm' : 'Confirm'}
            </button>
            <button type="button" style={linkButton} aria-label={`Rename ${item.name}`} onClick={() => setEditing(item)}>
              <EditIcon size={13} />
            </button>
            <button type="button" style={linkButton} onClick={() => void handleDismiss(item)}>
              Not recurring
            </button>
          </div>
        </td>
      </tr>
    ))
  }

  const head = (
    <thead>
      <tr>
        <th style={{ paddingLeft: 22 }}>Payment</th>
        <th>How often</th>
        <th className="align-right">Amount</th>
        <th className="align-right">Per month</th>
        <th>Next expected</th>
        <th style={{ paddingRight: 22 }} />
      </tr>
    </thead>
  )

  return (
    <div>
      <div className="page-header">
        <div>
          <h1>Recurring</h1>
          <div className="page-subtitle">Payments that repeat, found in your confirmed expenses.</div>
        </div>
        <Button variant="secondary" onClick={() => void load()} disabled={loading}>
          {loading ? 'Checking…' : 'Check again'}
        </Button>
      </div>

      {error && <div className="form-error-banner" style={{ marginBottom: 16 }}>{error}</div>}

      <div className="summary-grid" style={{ gridTemplateColumns: 'repeat(3, 1fr)' }}>
        <div className="summary-card">
          <div className="summary-label">Recurring payments</div>
          <div className="summary-value">{active.length}</div>
        </div>
        <div className="summary-card">
          <div className="summary-label">Per month</div>
          <div className="summary-value">{money('monthly')}</div>
        </div>
        <div className="summary-card">
          <div className="summary-label">Per year</div>
          <div className="summary-value">{money('yearly')}</div>
        </div>
      </div>

      {loading && items.length === 0 ? (
        <div className="loading-state">
          <span className="spinner" /> Looking for recurring payments…
        </div>
      ) : items.length === 0 ? (
        <div className="card">
          <div className="empty-state">
            Nothing recurring found yet. A payment shows up here once it has repeated at a regular interval, usually three times.
          </div>
        </div>
      ) : (
        <>
          {active.length > 0 && (
            <div className="card" style={{ padding: 0, marginBottom: 20 }}>
              <div className="table-wrap">
                <table>
                  {head}
                  <tbody>{renderRows(active)}</tbody>
                </table>
              </div>
            </div>
          )}
          {stopped.length > 0 && (
            <>
              <h2 style={{ fontSize: 15, margin: '8px 0 10px' }}>Stopped</h2>
              <div className="card" style={{ padding: 0 }}>
                <div className="table-wrap">
                  <table>
                    {head}
                    <tbody>{renderRows(stopped)}</tbody>
                  </table>
                </div>
              </div>
            </>
          )}
        </>
      )}

      {editing && (
        <RenameModal
          item={editing}
          onClose={() => setEditing(null)}
          onSaved={(updated) => {
            setItems((current) => current.map((i) => (i.id === updated.id ? updated : i)))
            setEditing(null)
          }}
        />
      )}
    </div>
  )
}

function RenameModal({ item, onClose, onSaved }: { item: RecurringExpense; onClose: () => void; onSaved: (updated: RecurringExpense) => void }) {
  const [name, setName] = useState(item.name)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setSaving(true)
    setError(null)
    try {
      onSaved(await recurringApi.update(item.id, { name }))
    } catch (err) {
      setError(err instanceof ApiRequestError ? err.message : 'Something went wrong. Please try again.')
      setSaving(false)
    }
  }

  return (
    <Modal title="Rename" onClose={onClose}>
      <form onSubmit={handleSubmit}>
        {error && <div className="form-error-banner">{error}</div>}
        <Field label="Name" required maxLength={255} value={name} onChange={(e) => setName(e.target.value)} />
        <div className="modal-actions">
          <Button type="button" variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button type="submit" disabled={saving || !name.trim()}>
            {saving ? 'Saving…' : 'Save'}
          </Button>
        </div>
      </form>
    </Modal>
  )
}
