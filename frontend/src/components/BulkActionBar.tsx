import { useState } from 'react'
import type { ReactNode } from 'react'
import type { BulkChoice, Category, Merchant } from '../api/types'
import { Button } from './ui/Button'

interface Props {
  count: number
  categories: Category[]
  merchants: Merchant[]
  /** Offer "remember this payee for next time" (only where a payee rule makes sense: statement rows). */
  offerRemember?: boolean
  /** Extra text or a link next to the count, e.g. "Select all 120 matching". */
  extra?: ReactNode
  busy: boolean
  onApply: (choice: BulkChoice) => void
  onClear: () => void
}

/** Appears when rows are ticked: choose a category (and/or a merchant) once and apply it to all of them. */
export function BulkActionBar({ count, categories, merchants, offerRemember, extra, busy, onApply, onClear }: Props) {
  const [categoryId, setCategoryId] = useState('')
  const [merchantId, setMerchantId] = useState('')
  const [remember, setRemember] = useState(true)

  if (count === 0) return null

  const sortedCategories = categories.filter((c) => c.active).sort((a, b) => a.name.localeCompare(b.name))
  const sortedMerchants = [...merchants].sort((a, b) => a.canonicalName.localeCompare(b.canonicalName))
  const ready = (categoryId !== '' || merchantId !== '') && !busy

  return (
    <div className="bulk-bar" role="region" aria-label="Bulk edit">
      <div className="bulk-count">
        <strong>{count}</strong> selected {extra}
      </div>
      <select aria-label="Category to set" value={categoryId} onChange={(e) => setCategoryId(e.target.value)} disabled={busy}>
        <option value="">Set category…</option>
        {sortedCategories.map((c) => (
          <option key={c.id} value={c.id}>
            {c.name}
          </option>
        ))}
      </select>
      <select aria-label="Merchant to set" value={merchantId} onChange={(e) => setMerchantId(e.target.value)} disabled={busy}>
        <option value="">Keep merchant</option>
        {sortedMerchants.map((m) => (
          <option key={m.id} value={m.id}>
            {m.canonicalName}
          </option>
        ))}
      </select>
      {offerRemember && (
        <label className="bulk-remember">
          <input type="checkbox" checked={remember} onChange={(e) => setRemember(e.target.checked)} disabled={busy} />
          Remember these payees
        </label>
      )}
      <Button
        small
        loading={busy}
        disabled={!ready}
        onClick={() =>
          onApply({
            categoryId: categoryId || undefined,
            merchantId: merchantId || undefined,
            rememberPayee: offerRemember ? remember : false,
          })
        }
      >
        Apply
      </Button>
      <Button small variant="secondary" onClick={onClear} disabled={busy}>
        Clear
      </Button>
    </div>
  )
}
