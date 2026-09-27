import { useState } from 'react'
import type { FormEvent } from 'react'
import { accountsApi } from '../api/accounts'
import { ApiRequestError } from '../api/client'
import type { Account, AccountType } from '../api/types'
import { BankIcon, PlusIcon } from '../components/icons'
import { Button } from '../components/ui/Button'
import { Field } from '../components/ui/Field'
import { Modal } from '../components/ui/Modal'
import { SelectField } from '../components/ui/SelectField'
import { useReferenceData } from '../data/ReferenceDataContext'
import { ACCOUNT_TYPE_LABELS, colorForKey } from '../lib/labels'

const ACCOUNT_TYPES: AccountType[] = ['BANK', 'CREDIT_CARD', 'CASH', 'WALLET', 'OTHER']
const CURRENCIES = ['INR', 'USD', 'EUR', 'GBP']

interface AccountFormState {
  name: string
  accountType: AccountType
  institutionName: string
  last4: string
  currency: string
}

const EMPTY_FORM: AccountFormState = { name: '', accountType: 'BANK', institutionName: '', last4: '', currency: 'INR' }

export function AccountsPage() {
  const { accounts, loading, refreshAccounts } = useReferenceData()
  const [modal, setModal] = useState<'create' | Account | null>(null)
  const [form, setForm] = useState<AccountFormState>(EMPTY_FORM)
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)
  const [pendingId, setPendingId] = useState<string | null>(null)

  function openCreate() {
    setForm(EMPTY_FORM)
    setError(null)
    setModal('create')
  }

  function openEdit(account: Account) {
    setForm({
      name: account.name,
      accountType: account.accountType,
      institutionName: account.institutionName ?? '',
      last4: account.last4 ?? '',
      currency: account.currency,
    })
    setError(null)
    setModal(account)
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setError(null)
    setSubmitting(true)
    try {
      if (modal === 'create') {
        await accountsApi.create({
          name: form.name,
          accountType: form.accountType,
          institutionName: form.institutionName || undefined,
          last4: form.last4 || undefined,
          currency: form.currency,
        })
      } else if (modal) {
        await accountsApi.update(modal.id, {
          name: form.name,
          institutionName: form.institutionName || undefined,
          last4: form.last4 || undefined,
        })
      }
      await refreshAccounts()
      setModal(null)
    } catch (err) {
      setError(err instanceof ApiRequestError ? err.message : 'Something went wrong. Please try again.')
    } finally {
      setSubmitting(false)
    }
  }

  async function handleDeactivate(account: Account) {
    if (!window.confirm(`Deactivate "${account.name}"? You can still see its past transactions.`)) return
    setPendingId(account.id)
    try {
      await accountsApi.deactivate(account.id)
      await refreshAccounts()
    } finally {
      setPendingId(null)
    }
  }

  const activeCount = accounts.filter((a) => a.active).length
  const currencies = new Set(accounts.map((a) => a.currency))

  return (
    <div>
      <div className="page-header">
        <div>
          <h1>Accounts</h1>
          <div className="page-subtitle">
            {accounts.length} account{accounts.length === 1 ? '' : 's'} &middot; {currencies.size} currenc
            {currencies.size === 1 ? 'y' : 'ies'} &middot; {activeCount} active
          </div>
        </div>
        <Button onClick={openCreate}>
          <PlusIcon />
          Add account
        </Button>
      </div>

      {loading ? (
        <div className="loading-state">
          <span className="spinner" /> Loading accounts&hellip;
        </div>
      ) : (
        <div className="accounts-grid">
          {accounts.map((account) => (
            <div className="account-card" key={account.id}>
              <div className="account-card-top">
                <div className="account-icon" style={{ background: colorForKey(account.accountType) }}>
                  <BankIcon size={20} />
                </div>
                <span className={`badge ${account.active ? 'positive' : ''}`}>{account.active ? 'Active' : 'Inactive'}</span>
              </div>
              <div>
                <div className="account-name">{account.name}</div>
                <div className="account-meta">
                  {account.institutionName ? `${account.institutionName} · ` : ''}
                  {ACCOUNT_TYPE_LABELS[account.accountType]}
                </div>
              </div>
              <div className="account-card-footer">
                <div className="account-number">{account.last4 ? `•••• ${account.last4}` : 'No card'}</div>
                <span className="badge">{account.currency}</span>
              </div>
              <div className="account-actions">
                <Button variant="secondary" small style={{ flex: 1 }} onClick={() => openEdit(account)}>
                  Edit
                </Button>
                {account.active && (
                  <Button
                    variant="danger"
                    small
                    style={{ flex: 1 }}
                    disabled={pendingId === account.id}
                    onClick={() => void handleDeactivate(account)}
                  >
                    {pendingId === account.id ? 'Deactivating…' : 'Deactivate'}
                  </Button>
                )}
              </div>
            </div>
          ))}

          <button type="button" className="add-account-card" onClick={openCreate}>
            <PlusIcon size={26} />
            Add another account
          </button>
        </div>
      )}

      {modal && (
        <Modal title={modal === 'create' ? 'Add account' : 'Edit account'} onClose={() => setModal(null)}>
          <form onSubmit={(e) => void handleSubmit(e)} style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
            {error && <div className="form-error-banner">{error}</div>}
            <Field
              label="Account name"
              required
              value={form.name}
              onChange={(e) => setForm({ ...form, name: e.target.value })}
              placeholder="HDFC Card"
            />
            {modal === 'create' && (
              <>
                <SelectField
                  label="Account type"
                  value={form.accountType}
                  onChange={(e) => setForm({ ...form, accountType: e.target.value as AccountType })}
                >
                  {ACCOUNT_TYPES.map((t) => (
                    <option key={t} value={t}>
                      {ACCOUNT_TYPE_LABELS[t]}
                    </option>
                  ))}
                </SelectField>
                <SelectField label="Currency" value={form.currency} onChange={(e) => setForm({ ...form, currency: e.target.value })}>
                  {CURRENCIES.map((c) => (
                    <option key={c} value={c}>
                      {c}
                    </option>
                  ))}
                </SelectField>
              </>
            )}
            <div className="field-row">
              <Field
                label="Institution (optional)"
                value={form.institutionName}
                onChange={(e) => setForm({ ...form, institutionName: e.target.value })}
                placeholder="HDFC Bank"
              />
              <Field
                label="Last 4 digits (optional)"
                maxLength={4}
                value={form.last4}
                onChange={(e) => setForm({ ...form, last4: e.target.value.replace(/\D/g, '') })}
                placeholder="4521"
              />
            </div>
            <div className="modal-actions">
              <Button type="button" variant="secondary" onClick={() => setModal(null)}>
                Cancel
              </Button>
              <Button type="submit" disabled={submitting}>
                {submitting ? 'Saving…' : modal === 'create' ? 'Add account' : 'Save changes'}
              </Button>
            </div>
          </form>
        </Modal>
      )}
    </div>
  )
}
