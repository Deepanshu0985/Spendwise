import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { ApiRequestError } from '../api/client'
import { goalsApi } from '../api/goals'
import type { SaveGoalRequest } from '../api/goals'
import type { Goal } from '../api/types'
import { EditIcon, PlusIcon, TrashIcon } from '../components/icons'
import { Button } from '../components/ui/Button'
import { Field } from '../components/ui/Field'
import { Modal } from '../components/ui/Modal'
import { useReferenceData } from '../data/ReferenceDataContext'
import { formatCurrency, formatDate } from '../lib/format'

function toRequest(goal: Goal, patch: Partial<SaveGoalRequest>): SaveGoalRequest {
  return {
    name: goal.name,
    targetAmount: goal.targetAmount,
    currentAmount: goal.currentAmount,
    ...(goal.targetDate ? { targetDate: goal.targetDate } : {}),
    ...patch,
  }
}

export function GoalsPage() {
  const [goals, setGoals] = useState<Goal[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [editing, setEditing] = useState<Goal | 'new' | null>(null)
  const [adding, setAdding] = useState<Goal | null>(null)

  async function load() {
    try {
      setGoals(await goalsApi.list())
    } catch (err) {
      setError(err instanceof ApiRequestError ? err.message : 'Something went wrong. Please try again.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void load()
  }, [])

  async function handleDelete(goal: Goal) {
    if (!window.confirm(`Delete the goal "${goal.name}"?`)) return
    setError(null)
    try {
      await goalsApi.remove(goal.id)
      setGoals((current) => current.filter((g) => g.id !== goal.id))
    } catch (err) {
      setError(err instanceof ApiRequestError ? err.message : 'Something went wrong. Please try again.')
    }
  }

  return (
    <div>
      <div className="page-header">
        <div>
          <h1>Goals</h1>
          <div className="page-subtitle">Save towards something, and see how far along you are.</div>
        </div>
        <Button onClick={() => setEditing('new')}>
          <PlusIcon />
          New goal
        </Button>
      </div>

      {error && <div className="form-error-banner" style={{ marginBottom: 16 }}>{error}</div>}

      {loading ? (
        <div className="loading-state">
          <span className="spinner" /> Loading goals…
        </div>
      ) : goals.length === 0 ? (
        <div className="card">
          <div className="empty-state">No goals yet. Create one, then add money to it as you save.</div>
        </div>
      ) : (
        <div style={{ display: 'grid', gap: 18 }}>
          {goals.map((goal) => (
            <div className="card" key={goal.id}>
              <div style={{ display: 'flex', justifyContent: 'space-between', gap: 12, flexWrap: 'wrap', marginBottom: 12 }}>
                <div>
                  <div style={{ fontWeight: 600, fontSize: 16 }}>{goal.name}</div>
                  <div className="page-subtitle" style={{ marginTop: 2 }}>
                    {goal.targetDate ? `Target ${formatDate(goal.targetDate)}` : 'No target date'}
                  </div>
                </div>
                <div style={{ display: 'flex', gap: 10, alignItems: 'center' }}>
                  {goal.status === 'ACHIEVED' ? <span className="badge positive">Achieved</span> : null}
                  {goal.overdue ? <span className="badge negative">Past its date</span> : null}
                  {goal.status === 'ACTIVE' && (
                    <Button variant="secondary" small onClick={() => setAdding(goal)}>
                      Add money
                    </Button>
                  )}
                  <button type="button" className="icon-button" aria-label={`Edit ${goal.name}`} onClick={() => setEditing(goal)}>
                    <EditIcon />
                  </button>
                  <button type="button" className="icon-button" aria-label={`Delete ${goal.name}`} onClick={() => void handleDelete(goal)}>
                    <TrashIcon />
                  </button>
                </div>
              </div>

              <div className="bar-row-labels" style={{ marginBottom: 5 }}>
                <span className="name">
                  {formatCurrency(goal.currentAmount, goal.currency)} of {formatCurrency(goal.targetAmount, goal.currency)}
                </span>
                <span className="amount">{goal.percentComplete}%</span>
              </div>
              <div className="bar-track">
                <div className="bar-fill" style={{ width: `${Math.max(2, goal.percentComplete)}%`, background: 'var(--positive)' }} />
              </div>
              <div className="page-subtitle" style={{ marginTop: 6 }}>
                {goal.status === 'ACHIEVED'
                  ? 'Target reached.'
                  : goal.requiredPerMonth !== null
                    ? `${formatCurrency(goal.remaining, goal.currency)} to go · set aside ${formatCurrency(goal.requiredPerMonth, goal.currency)} a month to make it`
                    : `${formatCurrency(goal.remaining, goal.currency)} to go`}
              </div>
            </div>
          ))}
        </div>
      )}

      {editing && (
        <GoalModal
          goal={editing === 'new' ? null : editing}
          onClose={() => setEditing(null)}
          onSaved={() => {
            setEditing(null)
            void load()
          }}
        />
      )}
      {adding && (
        <AddMoneyModal
          goal={adding}
          onClose={() => setAdding(null)}
          onSaved={() => {
            setAdding(null)
            void load()
          }}
        />
      )}
    </div>
  )
}

function GoalModal({ goal, onClose, onSaved }: { goal: Goal | null; onClose: () => void; onSaved: () => void }) {
  const { accounts } = useReferenceData()
  const [name, setName] = useState(goal?.name ?? '')
  const [targetAmount, setTargetAmount] = useState(goal ? String(goal.targetAmount) : '')
  const [currentAmount, setCurrentAmount] = useState(goal ? String(goal.currentAmount) : '0')
  const [targetDate, setTargetDate] = useState(goal?.targetDate ?? '')
  const [currency, setCurrency] = useState(goal?.currency ?? accounts[0]?.currency ?? 'INR')
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setError(null)
    setSaving(true)
    const body: SaveGoalRequest = {
      name: name.trim(),
      targetAmount: Number(targetAmount),
      currentAmount: Number(currentAmount || 0),
      ...(targetDate ? { targetDate } : {}),
      ...(goal ? {} : { currency }),
    }
    try {
      if (goal) await goalsApi.update(goal.id, body)
      else await goalsApi.create(body)
      onSaved()
    } catch (err) {
      setError(err instanceof ApiRequestError ? err.message : 'Something went wrong. Please try again.')
      setSaving(false)
    }
  }

  return (
    <Modal title={goal ? 'Edit goal' : 'New goal'} onClose={onClose}>
      <form onSubmit={handleSubmit}>
        {error && <div className="form-error-banner">{error}</div>}
        <Field label="Name" required maxLength={255} placeholder="New laptop" value={name} onChange={(e) => setName(e.target.value)} />
        <div style={{ display: 'flex', gap: 12 }}>
          <Field label="Target amount" type="number" required min="0.01" step="0.01" value={targetAmount} onChange={(e) => setTargetAmount(e.target.value)} />
          <Field label="Saved so far" type="number" min="0" step="0.01" value={currentAmount} onChange={(e) => setCurrentAmount(e.target.value)} />
        </div>
        <div style={{ display: 'flex', gap: 12 }}>
          <Field label="Target date (optional)" type="date" value={targetDate} onChange={(e) => setTargetDate(e.target.value)} />
          <Field label="Currency" required maxLength={3} disabled={!!goal} value={currency} onChange={(e) => setCurrency(e.target.value.toUpperCase())} />
        </div>
        <div className="modal-actions">
          <Button type="button" variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button type="submit" disabled={saving || !name.trim() || !targetAmount}>
            {saving ? 'Saving…' : 'Save goal'}
          </Button>
        </div>
      </form>
    </Modal>
  )
}

function AddMoneyModal({ goal, onClose, onSaved }: { goal: Goal; onClose: () => void; onSaved: () => void }) {
  const [amount, setAmount] = useState('')
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setError(null)
    setSaving(true)
    try {
      // Cents are summed as integers so 0.1 + 0.2 style drift never reaches the stored amount.
      const next = (Math.round(goal.currentAmount * 100) + Math.round(Number(amount) * 100)) / 100
      await goalsApi.update(goal.id, toRequest(goal, { currentAmount: next }))
      onSaved()
    } catch (err) {
      setError(err instanceof ApiRequestError ? err.message : 'Something went wrong. Please try again.')
      setSaving(false)
    }
  }

  return (
    <Modal title={`Add money to ${goal.name}`} onClose={onClose}>
      <form onSubmit={handleSubmit}>
        {error && <div className="form-error-banner">{error}</div>}
        <Field label="Amount to add" type="number" required min="0.01" step="0.01" autoFocus value={amount} onChange={(e) => setAmount(e.target.value)} />
        <div className="modal-actions">
          <Button type="button" variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button type="submit" disabled={saving || !amount}>
            {saving ? 'Saving…' : 'Add'}
          </Button>
        </div>
      </form>
    </Modal>
  )
}
