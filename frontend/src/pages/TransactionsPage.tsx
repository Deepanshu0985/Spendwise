import { useEffect, useMemo, useState } from 'react'
import type { FormEvent } from 'react'
import { ApiRequestError } from '../api/client'
import { merchantsApi } from '../api/merchants'
import { transactionsApi } from '../api/transactions'
import type { CreateTransactionRequest, UpdateTransactionRequest } from '../api/transactions'
import { transferApi } from '../api/transfers'
import type { PageMeta, Transaction, TransferKind } from '../api/types'
import { AddMultipleTransactionsModal } from '../components/AddMultipleTransactionsModal'
import { CalendarIcon, EditIcon, PlusIcon, TrashIcon } from '../components/icons'
import { Button } from '../components/ui/Button'
import { Field } from '../components/ui/Field'
import { Modal } from '../components/ui/Modal'
import { SelectField } from '../components/ui/SelectField'
import { useReferenceData } from '../data/ReferenceDataContext'
import { currentMonthRange, formatDate, formatDateRangeLabel, formatSignedCurrency, today } from '../lib/format'
import { isDebit, MANUALLY_ASSIGNABLE_TYPES, TRANSACTION_TYPE_LABELS } from '../lib/labels'

const NEW_MERCHANT = '__new__'
const NONE = ''

interface Filters {
  from: string
  to: string
  accountId: string
  categoryId: string
  type: string
  page: number
}

interface TxFormState {
  accountId: string
  categoryId: string
  merchantId: string
  transactionDate: string
  amount: string
  description: string
  transactionType: string
  status: string
}

function emptyForm(defaultAccountId: string): TxFormState {
  return {
    accountId: defaultAccountId,
    categoryId: NONE,
    merchantId: NONE,
    transactionDate: today(),
    amount: '',
    description: '',
    transactionType: 'EXPENSE',
    status: 'CONFIRMED',
  }
}

export function TransactionsPage() {
  const { accounts, categories, merchants, refreshMerchants } = useReferenceData()
  const [filters, setFilters] = useState<Filters>(() => ({ ...currentMonthRange(), accountId: NONE, categoryId: NONE, type: NONE, page: 0 }))
  const [transactions, setTransactions] = useState<Transaction[]>([])
  const [meta, setMeta] = useState<PageMeta | null>(null)
  const [loading, setLoading] = useState(true)
  const [listError, setListError] = useState<string | null>(null)

  const [modal, setModal] = useState<'create' | Transaction | null>(null)
  const [form, setForm] = useState<TxFormState>(() => emptyForm(accounts[0]?.id ?? NONE))
  const [newMerchantName, setNewMerchantName] = useState('')
  const [formError, setFormError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)
  const [pendingId, setPendingId] = useState<string | null>(null)

  const [transferOpen, setTransferOpen] = useState(false)
  const [multiOpen, setMultiOpen] = useState(false)

  const categoryById = useMemo(() => new Map(categories.map((c) => [c.id, c])), [categories])
  const merchantById = useMemo(() => new Map(merchants.map((m) => [m.id, m])), [merchants])
  const accountById = useMemo(() => new Map(accounts.map((a) => [a.id, a])), [accounts])
  const selectedAccount = accountById.get(form.accountId)

  useEffect(() => {
    let cancelled = false
    setLoading(true)
    setListError(null)
    transactionsApi
      .list({
        from: filters.from,
        to: filters.to,
        accountId: filters.accountId || undefined,
        categoryId: filters.categoryId || undefined,
        type: filters.type || undefined,
        page: filters.page,
        sort: 'transactionDate,desc',
      })
      .then((result) => {
        if (cancelled) return
        setTransactions(result.data)
        setMeta(result.meta)
      })
      .catch(() => {
        if (!cancelled) setListError('Could not load transactions. Please try again.')
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })
    return () => {
      cancelled = true
    }
  }, [filters])

  function openCreate() {
    setForm(emptyForm(accounts.find((a) => a.active)?.id ?? accounts[0]?.id ?? NONE))
    setNewMerchantName('')
    setFormError(null)
    setModal('create')
  }

  function openEdit(tx: Transaction) {
    setForm({
      accountId: tx.accountId,
      categoryId: tx.categoryId ?? NONE,
      merchantId: tx.merchantId ?? NONE,
      transactionDate: tx.transactionDate,
      amount: String(tx.amount),
      description: tx.description ?? '',
      transactionType: tx.transactionType,
      status: tx.status,
    })
    setNewMerchantName('')
    setFormError(null)
    setModal(tx)
  }

  async function reloadList() {
    const result = await transactionsApi.list({
      from: filters.from,
      to: filters.to,
      accountId: filters.accountId || undefined,
      categoryId: filters.categoryId || undefined,
      type: filters.type || undefined,
      page: filters.page,
      sort: 'transactionDate,desc',
    })
    setTransactions(result.data)
    setMeta(result.meta)
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setFormError(null)

    const amount = Number(form.amount)
    if (!form.accountId || !Number.isFinite(amount) || amount <= 0) {
      setFormError('Please choose an account and enter a positive amount.')
      return
    }

    setSubmitting(true)
    try {
      let merchantId = form.merchantId
      if (merchantId === NEW_MERCHANT) {
        if (!newMerchantName.trim()) {
          setFormError('Enter a name for the new merchant.')
          setSubmitting(false)
          return
        }
        merchantId = await createMerchant(newMerchantName.trim())
      }

      if (modal === 'create') {
        const body: CreateTransactionRequest = {
          accountId: form.accountId,
          categoryId: form.categoryId || undefined,
          merchantId: merchantId || undefined,
          transactionDate: form.transactionDate,
          amount,
          currency: selectedAccount?.currency ?? 'INR',
          description: form.description || undefined,
          transactionType: form.transactionType as CreateTransactionRequest['transactionType'],
        }
        await transactionsApi.create(body)
      } else if (modal) {
        const body: UpdateTransactionRequest = {
          accountId: form.accountId,
          categoryId: form.categoryId || undefined,
          merchantId: merchantId || undefined,
          transactionDate: form.transactionDate,
          amount,
          description: form.description || undefined,
          status: form.status,
        }
        await transactionsApi.update(modal.id, body)
      }
      await reloadList()
      setModal(null)
    } catch (err) {
      setFormError(err instanceof ApiRequestError ? err.message : 'Something went wrong. Please try again.')
    } finally {
      setSubmitting(false)
    }
  }

  async function createMerchant(name: string): Promise<string> {
    const created = await merchantsApi.create(name)
    await refreshMerchants()
    return created.id
  }

  async function handleDelete(tx: Transaction) {
    if (!window.confirm('Delete this transaction? It stays in your history as deleted, never removed.')) return
    setPendingId(tx.id)
    try {
      await transactionsApi.remove(tx.id)
      await reloadList()
    } finally {
      setPendingId(null)
    }
  }

  return (
    <div>
      <div className="page-header">
        <h1>Transactions</h1>
        <div style={{ display: 'flex', gap: 10 }}>
          <Button variant="secondary" onClick={() => setTransferOpen(true)}>
            Record transfer
          </Button>
          <Button variant="secondary" onClick={() => setMultiOpen(true)}>
            Add several
          </Button>
          <Button onClick={openCreate}>
            <PlusIcon />
            Add transaction
          </Button>
        </div>
      </div>

      <div className="toolbar">
        <select
          className="field-inline"
          value={filters.accountId}
          onChange={(e) => setFilters({ ...filters, accountId: e.target.value, page: 0 })}
        >
          <option value="">All accounts</option>
          {accounts.map((a) => (
            <option key={a.id} value={a.id}>
              {a.name}
            </option>
          ))}
        </select>
        <select
          className="field-inline"
          value={filters.categoryId}
          onChange={(e) => setFilters({ ...filters, categoryId: e.target.value, page: 0 })}
        >
          <option value="">All categories</option>
          {categories.map((c) => (
            <option key={c.id} value={c.id}>
              {c.name}
            </option>
          ))}
        </select>
        <select className="field-inline" value={filters.type} onChange={(e) => setFilters({ ...filters, type: e.target.value, page: 0 })}>
          <option value="">All types</option>
          {MANUALLY_ASSIGNABLE_TYPES.map((t) => (
            <option key={t} value={t}>
              {TRANSACTION_TYPE_LABELS[t]}
            </option>
          ))}
        </select>
        <div className="field-inline" style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <CalendarIcon />
          {formatDateRangeLabel(filters.from, filters.to)}
        </div>
      </div>

      {listError && <div className="form-error-banner" style={{ marginBottom: 16 }}>{listError}</div>}

      <div className="card" style={{ padding: 0, display: 'flex', flexDirection: 'column' }}>
        {loading ? (
          <div className="loading-state">
            <span className="spinner" /> Loading transactions&hellip;
          </div>
        ) : transactions.length === 0 ? (
          <div className="empty-state">No transactions in this range yet. Add your first one above.</div>
        ) : (
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th style={{ paddingLeft: 22 }}>Date</th>
                  <th>Description</th>
                  <th>Merchant</th>
                  <th>Category</th>
                  <th>Account</th>
                  <th className="align-right">Amount</th>
                  <th className="align-right" style={{ paddingRight: 22 }}>
                    Actions
                  </th>
                </tr>
              </thead>
              <tbody>
                {transactions.map((tx) => {
                  const negative = isDebit(tx.transactionType)
                  return (
                    <tr key={tx.id}>
                      <td style={{ paddingLeft: 22 }}>{formatDate(tx.transactionDate)}</td>
                      <td>{tx.description || <span style={{ color: 'var(--ink-faint)' }}>No description</span>}</td>
                      <td>{tx.merchantId ? (merchantById.get(tx.merchantId)?.canonicalName ?? '—') : '—'}</td>
                      <td>
                        {tx.categoryId ? (
                          <span className="badge">{categoryById.get(tx.categoryId)?.name ?? '—'}</span>
                        ) : (
                          '—'
                        )}
                      </td>
                      <td>{accountById.get(tx.accountId)?.name ?? '—'}</td>
                      <td className={`align-right ${negative ? 'amount-negative' : 'amount-positive'}`}>
                        {formatSignedCurrency(tx.amount, tx.currency, negative)}
                      </td>
                      <td style={{ paddingRight: 22 }}>
                        <div className="row-actions">
                          <button type="button" className="icon-button" aria-label="Edit transaction" onClick={() => openEdit(tx)}>
                            <EditIcon />
                          </button>
                          <button
                            type="button"
                            className="icon-button"
                            aria-label="Delete transaction"
                            disabled={pendingId === tx.id}
                            onClick={() => void handleDelete(tx)}
                          >
                            <TrashIcon />
                          </button>
                        </div>
                      </td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div>
        )}

        {meta && meta.totalElements > 0 && (
          <div className="pagination-bar">
            <span>
              Page {meta.page + 1} of {meta.totalPages} &middot; {meta.totalElements} transaction{meta.totalElements === 1 ? '' : 's'}
            </span>
            <div style={{ display: 'flex', gap: 8 }}>
              <Button
                variant="secondary"
                small
                disabled={filters.page === 0}
                onClick={() => setFilters({ ...filters, page: filters.page - 1 })}
              >
                Previous
              </Button>
              <Button
                variant="secondary"
                small
                disabled={filters.page + 1 >= meta.totalPages}
                onClick={() => setFilters({ ...filters, page: filters.page + 1 })}
              >
                Next
              </Button>
            </div>
          </div>
        )}
      </div>

      {modal && (
        <Modal title={modal === 'create' ? 'Add transaction' : 'Edit transaction'} onClose={() => setModal(null)}>
          <form onSubmit={(e) => void handleSubmit(e)} style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
            {formError && <div className="form-error-banner">{formError}</div>}

            <SelectField label="Account" required value={form.accountId} onChange={(e) => setForm({ ...form, accountId: e.target.value })}>
              {accounts.map((a) => (
                <option key={a.id} value={a.id}>
                  {a.name} ({a.currency})
                </option>
              ))}
            </SelectField>

            {modal === 'create' ? (
              <SelectField
                label="Type"
                value={form.transactionType}
                onChange={(e) => setForm({ ...form, transactionType: e.target.value })}
              >
                {MANUALLY_ASSIGNABLE_TYPES.map((t) => (
                  <option key={t} value={t}>
                    {TRANSACTION_TYPE_LABELS[t]}
                  </option>
                ))}
              </SelectField>
            ) : (
              <SelectField label="Status" value={form.status} onChange={(e) => setForm({ ...form, status: e.target.value })}>
                <option value="CONFIRMED">Confirmed</option>
                <option value="PENDING">Pending</option>
                <option value="IGNORED">Ignored</option>
              </SelectField>
            )}

            <div className="field-row">
              <Field
                label="Date"
                type="date"
                required
                value={form.transactionDate}
                onChange={(e) => setForm({ ...form, transactionDate: e.target.value })}
              />
              <Field
                label={`Amount (${selectedAccount?.currency ?? '—'})`}
                type="number"
                min="0.01"
                step="0.01"
                required
                value={form.amount}
                onChange={(e) => setForm({ ...form, amount: e.target.value })}
                placeholder="0.00"
              />
            </div>

            <Field
              label="Description (optional)"
              value={form.description}
              onChange={(e) => setForm({ ...form, description: e.target.value })}
              placeholder="Lunch with the team"
            />

            <SelectField label="Category (optional)" value={form.categoryId} onChange={(e) => setForm({ ...form, categoryId: e.target.value })}>
              <option value="">Uncategorized</option>
              {categories
                .filter((c) => c.active)
                .map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.name}
                  </option>
                ))}
            </SelectField>

            <SelectField
              label="Merchant (optional)"
              value={form.merchantId}
              onChange={(e) => setForm({ ...form, merchantId: e.target.value })}
            >
              <option value="">Unknown merchant</option>
              {merchants.map((m) => (
                <option key={m.id} value={m.id}>
                  {m.canonicalName}
                </option>
              ))}
              <option value={NEW_MERCHANT}>+ Add new merchant&hellip;</option>
            </SelectField>

            {form.merchantId === NEW_MERCHANT && (
              <Field
                label="New merchant name"
                required
                value={newMerchantName}
                onChange={(e) => setNewMerchantName(e.target.value)}
                placeholder="Swiggy"
              />
            )}

            <div className="modal-actions">
              <Button type="button" variant="secondary" onClick={() => setModal(null)}>
                Cancel
              </Button>
              <Button type="submit" disabled={submitting}>
                {submitting ? 'Saving…' : modal === 'create' ? 'Add transaction' : 'Save changes'}
              </Button>
            </div>
          </form>
        </Modal>
      )}

      {multiOpen && (
        <AddMultipleTransactionsModal
          onClose={() => setMultiOpen(false)}
          onDone={() => {
            setMultiOpen(false)
            void reloadList()
          }}
        />
      )}

      {transferOpen && (
        <TransferModal
          onClose={() => setTransferOpen(false)}
          onDone={() => {
            setTransferOpen(false)
            void reloadList()
          }}
        />
      )}
    </div>
  )
}

interface TransferFormState {
  fromAccountId: string
  toAccountId: string
  transactionDate: string
  amount: string
  kind: TransferKind
}

function TransferModal({ onClose, onDone }: { onClose: () => void; onDone: () => void }) {
  const { accounts } = useReferenceData()
  const [form, setForm] = useState<TransferFormState>({
    fromAccountId: accounts[0]?.id ?? NONE,
    toAccountId: accounts[1]?.id ?? NONE,
    transactionDate: today(),
    amount: '',
    kind: 'TRANSFER',
  })
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  const fromAccount = accounts.find((a) => a.id === form.fromAccountId)

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setError(null)
    const amount = Number(form.amount)
    if (form.fromAccountId === form.toAccountId) {
      setError('The two accounts must be different.')
      return
    }
    if (!Number.isFinite(amount) || amount <= 0) {
      setError('Enter a positive amount.')
      return
    }
    setSubmitting(true)
    try {
      await transferApi.create({
        fromAccountId: form.fromAccountId,
        toAccountId: form.toAccountId,
        transactionDate: form.transactionDate,
        amount,
        currency: fromAccount?.currency ?? 'INR',
        kind: form.kind,
      })
      onDone()
    } catch (err) {
      setError(err instanceof ApiRequestError ? err.message : 'Something went wrong. Please try again.')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <Modal title="Record a transfer" onClose={onClose}>
      <form onSubmit={(e) => void handleSubmit(e)} style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
        {error && <div className="form-error-banner">{error}</div>}
        <SelectField label="Kind" value={form.kind} onChange={(e) => setForm({ ...form, kind: e.target.value as TransferKind })}>
          <option value="TRANSFER">Transfer</option>
          <option value="CARD_PAYMENT">Card payment</option>
        </SelectField>
        <SelectField label="From account" value={form.fromAccountId} onChange={(e) => setForm({ ...form, fromAccountId: e.target.value })}>
          {accounts.map((a) => (
            <option key={a.id} value={a.id}>
              {a.name}
            </option>
          ))}
        </SelectField>
        <SelectField label="To account" value={form.toAccountId} onChange={(e) => setForm({ ...form, toAccountId: e.target.value })}>
          {accounts.map((a) => (
            <option key={a.id} value={a.id}>
              {a.name}
            </option>
          ))}
        </SelectField>
        <div className="field-row">
          <Field
            label="Date"
            type="date"
            required
            value={form.transactionDate}
            onChange={(e) => setForm({ ...form, transactionDate: e.target.value })}
          />
          <Field
            label={`Amount (${fromAccount?.currency ?? '—'})`}
            type="number"
            min="0.01"
            step="0.01"
            required
            value={form.amount}
            onChange={(e) => setForm({ ...form, amount: e.target.value })}
            placeholder="0.00"
          />
        </div>
        <div className="modal-actions">
          <Button type="button" variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button type="submit" disabled={submitting}>
            {submitting ? 'Recording…' : 'Record transfer'}
          </Button>
        </div>
      </form>
    </Modal>
  )
}
