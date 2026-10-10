import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { ApiRequestError } from '../api/client'
import { budgetsApi } from '../api/budgets'
import type { SaveBudgetRequest } from '../api/budgets'
import type { Budget, BudgetPeriodType, BudgetStatus } from '../api/types'
import { EditIcon, PlusIcon, TrashIcon } from '../components/icons'
import { Button } from '../components/ui/Button'
import { Field } from '../components/ui/Field'
import { Modal } from '../components/ui/Modal'
import { SelectField } from '../components/ui/SelectField'
import { useReferenceData } from '../data/ReferenceDataContext'
import { formatCurrency, formatDate } from '../lib/format'

const STATUS_LABELS: Record<BudgetStatus, string> = {
  ON_TRACK: 'On track',
  CLOSE_TO_LIMIT: 'Close to limit',
  OVER_BUDGET: 'Over budget',
}

const STATUS_COLORS: Record<BudgetStatus, string> = {
  ON_TRACK: 'var(--positive)',
  CLOSE_TO_LIMIT: '#b8862b',
  OVER_BUDGET: 'var(--negative)',
}

function StatusBadge({ status }: { status: BudgetStatus }) {
  return <span className={status === 'OVER_BUDGET' ? 'badge negative' : status === 'ON_TRACK' ? 'badge positive' : 'badge'}>{STATUS_LABELS[status]}</span>
}

function ProgressBar({ percent, status }: { percent: number; status: BudgetStatus }) {
  return (
    <div className="bar-track">
      <div className="bar-fill" style={{ width: `${Math.min(100, Math.max(2, percent))}%`, background: STATUS_COLORS[status] }} />
    </div>
  )
}

export function BudgetsPage() {
  const { categories } = useReferenceData()
  const [budgets, setBudgets] = useState<Budget[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [editing, setEditing] = useState<Budget | 'new' | null>(null)

  async function load() {
    try {
      setBudgets(await budgetsApi.list())
    } catch (err) {
      setError(err instanceof ApiRequestError ? err.message : 'Something went wrong. Please try again.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void load()
  }, [])

  async function handleDelete(budget: Budget) {
    if (!window.confirm(`Delete the budget "${budget.name}"? Your transactions are not affected.`)) return
    setError(null)
    try {
      await budgetsApi.remove(budget.id)
      setBudgets((current) => current.filter((b) => b.id !== budget.id))
    } catch (err) {
      setError(err instanceof ApiRequestError ? err.message : 'Something went wrong. Please try again.')
    }
  }

  const categoryName = (id: string) => categories.find((c) => c.id === id)?.name ?? '—'

  return (
    <div>
      <div className="page-header">
        <div>
          <h1>Budgets</h1>
          <div className="page-subtitle">Set a limit, and see how much of it you have used.</div>
        </div>
        <Button onClick={() => setEditing('new')}>
          <PlusIcon />
          New budget
        </Button>
      </div>

      {error && <div className="form-error-banner" style={{ marginBottom: 16 }}>{error}</div>}

      {loading ? (
        <div className="loading-state">
          <span className="spinner" /> Loading budgets…
        </div>
      ) : budgets.length === 0 ? (
        <div className="card">
          <div className="empty-state">No budgets yet. Create one to track your spending against a limit.</div>
        </div>
      ) : (
        <div style={{ display: 'grid', gap: 18 }}>
          {budgets.map((budget) => {
            const p = budget.progress
            return (
              <div className="card" key={budget.id}>
                <div style={{ display: 'flex', justifyContent: 'space-between', gap: 12, flexWrap: 'wrap', marginBottom: 12 }}>
                  <div>
                    <div style={{ fontWeight: 600, fontSize: 16 }}>{budget.name}</div>
                    <div className="page-subtitle" style={{ marginTop: 2 }}>
                      {budget.periodType === 'MONTHLY' ? 'Every month · ' : ''}
                      {formatDate(p.windowStart)} – {formatDate(p.windowEnd)}
                    </div>
                  </div>
                  <div style={{ display: 'flex', gap: 10, alignItems: 'center' }}>
                    <StatusBadge status={p.status} />
                    <button type="button" className="icon-button" aria-label={`Edit ${budget.name}`} onClick={() => setEditing(budget)}>
                      <EditIcon />
                    </button>
                    <button type="button" className="icon-button" aria-label={`Delete ${budget.name}`} onClick={() => void handleDelete(budget)}>
                      <TrashIcon />
                    </button>
                  </div>
                </div>

                <div className="bar-row-labels" style={{ marginBottom: 5 }}>
                  <span className="name">
                    {formatCurrency(p.totalSpent, budget.currency)} of {formatCurrency(budget.totalLimit, budget.currency)}
                  </span>
                  <span className="amount">{p.percentUsed}%</span>
                </div>
                <ProgressBar percent={p.percentUsed} status={p.status} />
                <div className="page-subtitle" style={{ marginTop: 6 }}>
                  {p.remaining >= 0
                    ? `${formatCurrency(p.remaining, budget.currency)} left`
                    : `${formatCurrency(-p.remaining, budget.currency)} over`}
                </div>

                {p.categories.length > 0 && (
                  <div style={{ marginTop: 18, display: 'grid', gap: 12 }}>
                    {p.categories.map((line) => (
                      <div className="bar-row" style={{ margin: 0 }} key={line.categoryId}>
                        <div className="bar-row-labels">
                          <span className="name">{categoryName(line.categoryId)}</span>
                          <span className="amount">
                            {formatCurrency(line.spent, budget.currency)} / {formatCurrency(line.limitAmount, budget.currency)}
                          </span>
                        </div>
                        <ProgressBar percent={line.percentUsed} status={line.status} />
                      </div>
                    ))}
                  </div>
                )}
              </div>
            )
          })}
        </div>
      )}

      {editing && (
        <BudgetModal
          budget={editing === 'new' ? null : editing}
          onClose={() => setEditing(null)}
          onSaved={() => {
            setEditing(null)
            void load()
          }}
        />
      )}
    </div>
  )
}

interface LimitRow {
  categoryId: string
  limitAmount: string
}

function BudgetModal({ budget, onClose, onSaved }: { budget: Budget | null; onClose: () => void; onSaved: () => void }) {
  const { accounts, categories } = useReferenceData()
  const expenseCategories = categories.filter((c) => c.categoryType === 'EXPENSE' && c.active)
  const [name, setName] = useState(budget?.name ?? '')
  const [periodType, setPeriodType] = useState<BudgetPeriodType>(budget?.periodType ?? 'MONTHLY')
  const [startDate, setStartDate] = useState(budget?.periodType === 'CUSTOM' ? budget.startDate : '')
  const [endDate, setEndDate] = useState(budget?.endDate ?? '')
  const [totalLimit, setTotalLimit] = useState(budget ? String(budget.totalLimit) : '')
  const [currency, setCurrency] = useState(budget?.currency ?? accounts[0]?.currency ?? 'INR')
  const [limits, setLimits] = useState<LimitRow[]>(
    budget?.progress.categories.map((c) => ({ categoryId: c.categoryId, limitAmount: String(c.limitAmount) })) ?? [],
  )
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const used = new Set(limits.map((l) => l.categoryId))

  function updateLimit(index: number, patch: Partial<LimitRow>) {
    setLimits((current) => current.map((row, i) => (i === index ? { ...row, ...patch } : row)))
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setError(null)
    const body: SaveBudgetRequest = {
      name: name.trim(),
      periodType,
      totalLimit: Number(totalLimit),
      categoryLimits: limits.filter((l) => l.categoryId && l.limitAmount).map((l) => ({ categoryId: l.categoryId, limitAmount: Number(l.limitAmount) })),
    }
    if (periodType === 'CUSTOM') {
      body.startDate = startDate
      body.endDate = endDate
    }
    if (!budget) body.currency = currency
    setSaving(true)
    try {
      if (budget) await budgetsApi.update(budget.id, body)
      else await budgetsApi.create(body)
      onSaved()
    } catch (err) {
      setError(err instanceof ApiRequestError ? err.message : 'Something went wrong. Please try again.')
      setSaving(false)
    }
  }

  return (
    <Modal title={budget ? 'Edit budget' : 'New budget'} onClose={onClose} wide>
      <form onSubmit={handleSubmit}>
        {error && <div className="form-error-banner">{error}</div>}
        <Field label="Name" required maxLength={255} placeholder="Monthly spending" value={name} onChange={(e) => setName(e.target.value)} />
        <SelectField label="Period" value={periodType} onChange={(e) => setPeriodType(e.target.value as BudgetPeriodType)}>
          <option value="MONTHLY">Every month</option>
          <option value="CUSTOM">Custom dates</option>
        </SelectField>
        {periodType === 'CUSTOM' && (
          <div style={{ display: 'flex', gap: 12 }}>
            <Field label="Start date" type="date" required value={startDate} onChange={(e) => setStartDate(e.target.value)} />
            <Field label="End date" type="date" required value={endDate} onChange={(e) => setEndDate(e.target.value)} />
          </div>
        )}
        <div style={{ display: 'flex', gap: 12 }}>
          <Field label="Total limit" type="number" required min="0.01" step="0.01" value={totalLimit} onChange={(e) => setTotalLimit(e.target.value)} />
          <Field label="Currency" required maxLength={3} disabled={!!budget} value={currency} onChange={(e) => setCurrency(e.target.value.toUpperCase())} />
        </div>

        <div style={{ fontWeight: 600, margin: '14px 0 8px' }}>Limits per category (optional)</div>
        {limits.map((row, index) => (
          <div key={index} style={{ display: 'flex', gap: 10, alignItems: 'flex-end', marginBottom: 8 }}>
            <SelectField label="Category" value={row.categoryId} onChange={(e) => updateLimit(index, { categoryId: e.target.value })}>
              <option value="">Choose…</option>
              {expenseCategories
                .filter((c) => c.id === row.categoryId || !used.has(c.id))
                .map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.name}
                  </option>
                ))}
            </SelectField>
            <Field label="Limit" type="number" min="0.01" step="0.01" value={row.limitAmount} onChange={(e) => updateLimit(index, { limitAmount: e.target.value })} />
            <button type="button" className="icon-button" aria-label="Remove limit" onClick={() => setLimits((current) => current.filter((_, i) => i !== index))}>
              <TrashIcon />
            </button>
          </div>
        ))}
        <Button type="button" variant="secondary" small onClick={() => setLimits((current) => [...current, { categoryId: '', limitAmount: '' }])}>
          <PlusIcon />
          Add a category limit
        </Button>

        <div className="modal-actions">
          <Button type="button" variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button type="submit" disabled={saving || !name.trim() || !totalLimit}>
            {saving ? 'Saving…' : 'Save budget'}
          </Button>
        </div>
      </form>
    </Modal>
  )
}
