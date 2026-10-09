import { useState } from 'react'
import type { FormEvent } from 'react'
import { ApiRequestError } from '../api/client'
import { transactionsApi } from '../api/transactions'
import type { CreateTransactionRequest } from '../api/transactions'
import { useReferenceData } from '../data/ReferenceDataContext'
import { today } from '../lib/format'
import { MANUALLY_ASSIGNABLE_TYPES, TRANSACTION_TYPE_LABELS } from '../lib/labels'
import { PlusIcon, TrashIcon } from './icons'
import { Button } from './ui/Button'
import { Field } from './ui/Field'
import { Modal } from './ui/Modal'
import { SelectField } from './ui/SelectField'

interface Row {
  // One idempotency key per row, kept for as long as the row is unchanged: if a submit fails
  // part-way and the user retries, rows that already saved come back from the server's stored
  // response instead of being created twice. Editing a row mints a new key.
  key: string
  transactionType: string
  amount: string
  description: string
  categoryId: string
  saved: boolean
  error: string | null
}

function newRow(): Row {
  return { key: crypto.randomUUID(), transactionType: 'EXPENSE', amount: '', description: '', categoryId: '', saved: false, error: null }
}

const INITIAL_ROWS = 3

export function AddMultipleTransactionsModal({ onClose, onDone }: { onClose: () => void; onDone: () => void }) {
  const { accounts, categories } = useReferenceData()
  const [accountId, setAccountId] = useState(() => accounts.find((a) => a.active)?.id ?? accounts[0]?.id ?? '')
  const [date, setDate] = useState(today())
  const [rows, setRows] = useState<Row[]>(() => Array.from({ length: INITIAL_ROWS }, newRow))
  const [submitting, setSubmitting] = useState(false)
  const [formError, setFormError] = useState<string | null>(null)

  const account = accounts.find((a) => a.id === accountId)
  const anySaved = rows.some((r) => r.saved)

  function updateRow(index: number, change: Partial<Row>) {
    setRows((current) =>
      current.map((row, i) => (i === index ? { ...row, ...change, key: crypto.randomUUID(), error: null } : row)),
    )
  }

  function removeRow(index: number) {
    setRows((current) => current.filter((_, i) => i !== index))
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setFormError(null)

    const pending = rows.map((row, index) => ({ row, index })).filter(({ row }) => !row.saved && row.amount.trim() !== '')
    if (!accountId) {
      setFormError('Choose an account.')
      return
    }
    if (pending.length === 0) {
      setFormError(anySaved ? 'Everything you entered is already saved.' : 'Enter an amount on at least one row.')
      return
    }
    const invalid = pending.find(({ row }) => !Number.isFinite(Number(row.amount)) || Number(row.amount) <= 0)
    if (invalid) {
      setFormError(`Row ${invalid.index + 1}: enter a positive amount.`)
      return
    }

    setSubmitting(true)
    const outcomes = new Map<number, Partial<Row>>()
    for (const { row, index } of pending) {
      const body: CreateTransactionRequest = {
        accountId,
        categoryId: row.categoryId || undefined,
        transactionDate: date,
        amount: Number(row.amount),
        currency: account?.currency ?? 'INR',
        description: row.description.trim() || undefined,
        transactionType: row.transactionType as CreateTransactionRequest['transactionType'],
      }
      try {
        await transactionsApi.create(body, row.key)
        outcomes.set(index, { saved: true, error: null })
      } catch (err) {
        outcomes.set(index, {
          error: err instanceof ApiRequestError ? err.message : 'Could not reach the server - check your connection and try again.',
        })
      }
    }
    // Rows are merged by position at the end of the loop, not with a stale snapshot per row.
    const next = rows.map((row, i) => (outcomes.has(i) ? { ...row, ...outcomes.get(i) } : row))
    setRows(next)
    setSubmitting(false)

    const failed = [...outcomes.values()].filter((o) => o.error).length
    if (failed === 0) {
      onDone()
    } else {
      setFormError(`${failed} row${failed === 1 ? '' : 's'} could not be saved. The rest are saved - fix the marked rows and try again.`)
    }
  }

  const pendingCount = rows.filter((r) => !r.saved && r.amount.trim() !== '').length

  return (
    <Modal title="Add several transactions" onClose={submitting ? () => undefined : onClose} wide>
      <form onSubmit={(e) => void handleSubmit(e)} style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
        {formError && <div className="form-error-banner">{formError}</div>}

        <div className="field-row">
          <Field label="Date" type="date" required value={date} disabled={anySaved || submitting} onChange={(e) => setDate(e.target.value)} />
          <SelectField label="Account" value={accountId} disabled={anySaved || submitting} onChange={(e) => setAccountId(e.target.value)}>
            {accounts
              .filter((a) => a.active)
              .map((a) => (
                <option key={a.id} value={a.id}>
                  {a.name} ({a.currency})
                </option>
              ))}
          </SelectField>
        </div>

        <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
          {rows.map((row, index) => (
            <div key={index} style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
              <div className="multi-row">
                <select
                  className="field-inline"
                  aria-label={`Row ${index + 1} type`}
                  value={row.transactionType}
                  disabled={row.saved || submitting}
                  onChange={(e) => updateRow(index, { transactionType: e.target.value })}
                >
                  {MANUALLY_ASSIGNABLE_TYPES.map((t) => (
                    <option key={t} value={t}>
                      {TRANSACTION_TYPE_LABELS[t]}
                    </option>
                  ))}
                </select>
                <input
                  className="field-inline"
                  aria-label={`Row ${index + 1} amount`}
                  type="number"
                  min="0.01"
                  step="0.01"
                  placeholder="Amount"
                  value={row.amount}
                  disabled={row.saved || submitting}
                  onChange={(e) => updateRow(index, { amount: e.target.value })}
                />
                <input
                  className="field-inline"
                  aria-label={`Row ${index + 1} description`}
                  placeholder="Description (optional)"
                  value={row.description}
                  disabled={row.saved || submitting}
                  onChange={(e) => updateRow(index, { description: e.target.value })}
                />
                <select
                  className="field-inline"
                  aria-label={`Row ${index + 1} category`}
                  value={row.categoryId}
                  disabled={row.saved || submitting}
                  onChange={(e) => updateRow(index, { categoryId: e.target.value })}
                >
                  <option value="">Uncategorized</option>
                  {categories
                    .filter((c) => c.active)
                    .map((c) => (
                      <option key={c.id} value={c.id}>
                        {c.name}
                      </option>
                    ))}
                </select>
                {row.saved ? (
                  <span className="badge positive" title="Saved">
                    Saved
                  </span>
                ) : (
                  <button
                    type="button"
                    aria-label={`Remove row ${index + 1}`}
                    disabled={rows.length === 1 || submitting}
                    onClick={() => removeRow(index)}
                    style={{ background: 'none', border: 'none', cursor: 'pointer', color: 'var(--ink-muted)' }}
                  >
                    <TrashIcon />
                  </button>
                )}
              </div>
              {row.error && <span className="field-error">Row {index + 1}: {row.error}</span>}
            </div>
          ))}
        </div>

        <div>
          <Button type="button" variant="secondary" disabled={submitting} onClick={() => setRows((current) => [...current, newRow()])}>
            <PlusIcon />
            Add another row
          </Button>
        </div>

        <div className="modal-actions">
          <Button type="button" variant="secondary" disabled={submitting} onClick={onClose}>
            {anySaved ? 'Close' : 'Cancel'}
          </Button>
          <Button type="submit" disabled={submitting || pendingCount === 0}>
            {submitting ? 'Saving…' : `Save ${pendingCount || ''} transaction${pendingCount === 1 ? '' : 's'}`.replace('  ', ' ')}
          </Button>
        </div>
      </form>
    </Modal>
  )
}
