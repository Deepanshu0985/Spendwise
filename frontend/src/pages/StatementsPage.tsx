import { useEffect, useState } from 'react'
import type { ChangeEvent, FormEvent } from 'react'
import { ApiRequestError } from '../api/client'
import { statementsApi } from '../api/statements'
import type { PageMeta, Statement, StatementStatus, StatementTransaction } from '../api/types'
import { EditIcon, FileTextIcon, UploadIcon } from '../components/icons'
import { Button } from '../components/ui/Button'
import { Field } from '../components/ui/Field'
import { Modal } from '../components/ui/Modal'
import { SelectField } from '../components/ui/SelectField'
import { useReferenceData } from '../data/ReferenceDataContext'
import { formatDate, formatSignedCurrency } from '../lib/format'
import { isDebit, MANUALLY_ASSIGNABLE_TYPES, TRANSACTION_TYPE_LABELS } from '../lib/labels'

const STATUS_LABELS: Record<StatementStatus, string> = {
  UPLOADED: 'Uploaded',
  PROCESSING: 'Processing…',
  READY_FOR_REVIEW: 'Ready for review',
  IMPORTED: 'Imported',
  FAILED: 'Failed',
}

function statusBadgeClass(status: StatementStatus): string {
  if (status === 'IMPORTED') return 'badge positive'
  if (status === 'FAILED') return 'badge negative'
  return 'badge'
}

export function StatementsPage() {
  const { accounts } = useReferenceData()
  const [statements, setStatements] = useState<Statement[]>([])
  const [meta, setMeta] = useState<PageMeta | null>(null)
  const [loading, setLoading] = useState(true)
  const [uploadOpen, setUploadOpen] = useState(false)
  const [selectedId, setSelectedId] = useState<string | null>(null)

  async function reload() {
    setLoading(true)
    try {
      const result = await statementsApi.list()
      setStatements(result.data)
      setMeta(result.meta)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void reload()
  }, [])

  if (selectedId) {
    return (
      <StatementReview
        statementId={selectedId}
        onBack={() => {
          setSelectedId(null)
          void reload()
        }}
      />
    )
  }

  const accountName = (id: string) => accounts.find((a) => a.id === id)?.name ?? '—'

  return (
    <div>
      <div className="page-header">
        <h1>Statements</h1>
        <Button onClick={() => setUploadOpen(true)}>
          <UploadIcon />
          Upload statement
        </Button>
      </div>

      <div className="card" style={{ padding: 0, display: 'flex', flexDirection: 'column' }}>
        {loading ? (
          <div className="loading-state">
            <span className="spinner" /> Loading statements…
          </div>
        ) : statements.length === 0 ? (
          <div className="empty-state">No statements uploaded yet. Upload a bank PDF to get started.</div>
        ) : (
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th style={{ paddingLeft: 22 }}>File</th>
                  <th>Account</th>
                  <th>Period</th>
                  <th>Status</th>
                  <th style={{ paddingRight: 22 }}>Uploaded</th>
                </tr>
              </thead>
              <tbody>
                {statements.map((s) => (
                  <tr key={s.id} onClick={() => setSelectedId(s.id)} style={{ cursor: 'pointer' }}>
                    <td style={{ paddingLeft: 22, display: 'flex', alignItems: 'center', gap: 8 }}>
                      <FileTextIcon size={16} />
                      {s.fileName}
                    </td>
                    <td>{accountName(s.accountId)}</td>
                    <td>{s.periodStart && s.periodEnd ? `${formatDate(s.periodStart)} – ${formatDate(s.periodEnd)}` : '—'}</td>
                    <td>
                      <span className={statusBadgeClass(s.status)}>{STATUS_LABELS[s.status]}</span>
                    </td>
                    <td style={{ paddingRight: 22 }}>{new Date(s.createdAt).toLocaleDateString('en-IN')}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        {meta && meta.totalElements > 0 && (
          <div className="pagination-bar">
            <span>
              {meta.totalElements} statement{meta.totalElements === 1 ? '' : 's'}
            </span>
          </div>
        )}
      </div>

      {uploadOpen && (
        <UploadStatementModal
          onClose={() => setUploadOpen(false)}
          onUploaded={(statement) => {
            setUploadOpen(false)
            setSelectedId(statement.id)
          }}
        />
      )}
    </div>
  )
}

function UploadStatementModal({ onClose, onUploaded }: { onClose: () => void; onUploaded: (statement: Statement) => void }) {
  const { accounts } = useReferenceData()
  const [accountId, setAccountId] = useState(accounts[0]?.id ?? '')
  const [file, setFile] = useState<File | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  function handleFileChange(e: ChangeEvent<HTMLInputElement>) {
    setFile(e.target.files?.[0] ?? null)
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setError(null)
    if (!file) {
      setError('Choose a PDF statement to upload.')
      return
    }
    if (!accountId) {
      setError('Choose which account this statement belongs to.')
      return
    }
    setSubmitting(true)
    try {
      const statement = await statementsApi.upload(accountId, file)
      onUploaded(statement)
    } catch (err) {
      setError(err instanceof ApiRequestError ? err.message : 'Something went wrong. Please try again.')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <Modal title="Upload a statement" onClose={onClose}>
      <form onSubmit={(e) => void handleSubmit(e)} style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
        {error && <div className="form-error-banner">{error}</div>}

        <SelectField label="Account" required value={accountId} onChange={(e) => setAccountId(e.target.value)}>
          {accounts.map((a) => (
            <option key={a.id} value={a.id}>
              {a.name} ({a.currency})
            </option>
          ))}
        </SelectField>

        <div className="field">
          <label htmlFor="statement-file">Statement PDF</label>
          <input id="statement-file" type="file" accept="application/pdf" required onChange={handleFileChange} />
          <span style={{ fontSize: 12, color: 'var(--muted, #767268)' }}>
            HDFC Bank, SBI, Axis Bank, Bank of Baroda, Ujjivan Small Finance Bank or Paytm Wallet statements only.
          </span>
        </div>

        <div className="modal-actions">
          <Button type="button" variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button type="submit" disabled={submitting}>
            {submitting ? 'Uploading…' : 'Upload'}
          </Button>
        </div>
      </form>
    </Modal>
  )
}

function StatementReview({ statementId, onBack }: { statementId: string; onBack: () => void }) {
  const { accounts, categories, merchants } = useReferenceData()
  const [statement, setStatement] = useState<Statement | null>(null)
  const [rows, setRows] = useState<StatementTransaction[]>([])
  const [loading, setLoading] = useState(true)
  const [editingRow, setEditingRow] = useState<StatementTransaction | null>(null)
  const [confirming, setConfirming] = useState(false)
  const [retrying, setRetrying] = useState(false)
  const [actionError, setActionError] = useState<string | null>(null)

  async function reload() {
    setLoading(true)
    try {
      const [s, r] = await Promise.all([statementsApi.get(statementId), statementsApi.getTransactions(statementId)])
      setStatement(s)
      setRows(r)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void reload()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [statementId])

  async function handleConfirm() {
    setActionError(null)
    setConfirming(true)
    try {
      await statementsApi.confirm(statementId)
      await reload()
    } catch (err) {
      setActionError(err instanceof ApiRequestError ? err.message : 'Something went wrong. Please try again.')
    } finally {
      setConfirming(false)
    }
  }

  async function handleRetry() {
    setActionError(null)
    setRetrying(true)
    try {
      await statementsApi.retry(statementId)
      await reload()
    } catch (err) {
      setActionError(err instanceof ApiRequestError ? err.message : 'Something went wrong. Please try again.')
    } finally {
      setRetrying(false)
    }
  }

  const accountName = statement ? (accounts.find((a) => a.id === statement.accountId)?.name ?? '—') : '—'
  const categoryName = (id: string | null) => (id ? (categories.find((c) => c.id === id)?.name ?? '—') : '—')
  const merchantName = (id: string | null) => (id ? (merchants.find((m) => m.id === id)?.canonicalName ?? '—') : '—')

  return (
    <div>
      <div className="page-header">
        <div>
          <button type="button" onClick={onBack} style={{ background: 'none', border: 'none', padding: 0, marginBottom: 6, cursor: 'pointer', color: 'var(--accent)' }}>
            ← Back to statements
          </button>
          <h1>{statement?.fileName ?? 'Statement'}</h1>
          <div className="page-subtitle">
            {accountName} {statement?.periodStart && statement.periodEnd ? `· ${formatDate(statement.periodStart)} – ${formatDate(statement.periodEnd)}` : ''}
          </div>
        </div>
        {statement && (
          <div style={{ display: 'flex', gap: 10, alignItems: 'center' }}>
            <span className={statusBadgeClass(statement.status)}>{STATUS_LABELS[statement.status]}</span>
            {statement.status === 'FAILED' && (
              <Button variant="secondary" onClick={() => void handleRetry()} disabled={retrying}>
                {retrying ? 'Retrying…' : 'Retry'}
              </Button>
            )}
            {statement.status === 'READY_FOR_REVIEW' && (
              <Button onClick={() => void handleConfirm()} disabled={confirming}>
                {confirming ? 'Confirming…' : 'Confirm import'}
              </Button>
            )}
          </div>
        )}
      </div>

      {actionError && <div className="form-error-banner" style={{ marginBottom: 16 }}>{actionError}</div>}
      {statement?.status === 'FAILED' && statement.errorMessage && (
        <div className="form-error-banner" style={{ marginBottom: 16 }}>{statement.errorMessage}</div>
      )}

      {loading ? (
        <div className="loading-state">
          <span className="spinner" /> Loading…
        </div>
      ) : rows.length === 0 ? (
        <div className="card">
          <div className="empty-state">
            {statement?.status === 'PROCESSING' || statement?.status === 'UPLOADED'
              ? 'Still processing this statement…'
              : 'No transactions were parsed from this statement.'}
          </div>
        </div>
      ) : (
        <div className="card" style={{ padding: 0 }}>
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th style={{ paddingLeft: 22 }}>Date</th>
                  <th>Description</th>
                  <th>Category</th>
                  <th>Merchant</th>
                  <th>Type</th>
                  <th className="align-right">Amount</th>
                  <th className="align-right" style={{ paddingRight: 22 }}>
                    Confidence
                  </th>
                </tr>
              </thead>
              <tbody>
                {rows.map((row) => {
                  const negative = isDebit(row.suggestedTransactionType)
                  const editable = statement?.status === 'READY_FOR_REVIEW'
                  return (
                    <tr
                      key={row.id}
                      onClick={() => editable && setEditingRow(row)}
                      style={editable ? { cursor: 'pointer' } : undefined}
                    >
                      <td style={{ paddingLeft: 22 }}>{formatDate(row.transactionDate)}</td>
                      <td>
                        {row.normalizedDescription ?? row.rawDescription}
                        {editable && <EditIcon size={13} />}
                      </td>
                      <td>
                        {row.suggestedCategoryId ? <span className="badge">{categoryName(row.suggestedCategoryId)}</span> : '—'}
                      </td>
                      <td>{merchantName(row.suggestedMerchantId)}</td>
                      <td>{TRANSACTION_TYPE_LABELS[row.suggestedTransactionType] ?? row.suggestedTransactionType}</td>
                      <td className={`align-right ${negative ? 'amount-negative' : 'amount-positive'}`}>
                        {formatSignedCurrency(row.amount, row.currency, negative)}
                      </td>
                      <td className="align-right" style={{ paddingRight: 22 }}>
                        {Math.round(row.confidenceScore * 100)}%
                      </td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {editingRow && (
        <EditStagedRowModal
          statementId={statementId}
          row={editingRow}
          onClose={() => setEditingRow(null)}
          onSaved={() => {
            setEditingRow(null)
            void reload()
          }}
        />
      )}
    </div>
  )
}

function EditStagedRowModal({
  statementId,
  row,
  onClose,
  onSaved,
}: {
  statementId: string
  row: StatementTransaction
  onClose: () => void
  onSaved: () => void
}) {
  const { categories, merchants } = useReferenceData()
  const [transactionDate, setTransactionDate] = useState(row.transactionDate)
  const [amount, setAmount] = useState(String(row.amount))
  const [categoryId, setCategoryId] = useState(row.suggestedCategoryId ?? '')
  const [merchantId, setMerchantId] = useState(row.suggestedMerchantId ?? '')
  const [transactionType, setTransactionType] = useState(row.suggestedTransactionType)
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setError(null)
    const amountValue = Number(amount)
    if (!Number.isFinite(amountValue) || amountValue <= 0) {
      setError('Enter a positive amount.')
      return
    }
    setSubmitting(true)
    try {
      await statementsApi.updateStagedTransaction(statementId, row.id, {
        transactionDate,
        amount: amountValue,
        categoryId: categoryId || undefined,
        merchantId: merchantId || undefined,
        transactionType,
      })
      onSaved()
    } catch (err) {
      setError(err instanceof ApiRequestError ? err.message : 'Something went wrong. Please try again.')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <Modal title="Review transaction" onClose={onClose}>
      <form onSubmit={(e) => void handleSubmit(e)} style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
        {error && <div className="form-error-banner">{error}</div>}

        <div className="field">
          <label>Original description</label>
          <div style={{ color: 'var(--muted, #767268)' }}>{row.rawDescription}</div>
        </div>

        <div className="field-row">
          <Field label="Date" type="date" required value={transactionDate} onChange={(e) => setTransactionDate(e.target.value)} />
          <Field
            label={`Amount (${row.currency})`}
            type="number"
            min="0.01"
            step="0.01"
            required
            value={amount}
            onChange={(e) => setAmount(e.target.value)}
          />
        </div>

        <SelectField
          label="Type"
          value={transactionType}
          onChange={(e) => setTransactionType(e.target.value as typeof transactionType)}
        >
          {MANUALLY_ASSIGNABLE_TYPES.map((t) => (
            <option key={t} value={t}>
              {TRANSACTION_TYPE_LABELS[t]}
            </option>
          ))}
        </SelectField>

        <SelectField label="Category (optional)" value={categoryId} onChange={(e) => setCategoryId(e.target.value)}>
          <option value="">Uncategorized</option>
          {categories
            .filter((c) => c.active)
            .map((c) => (
              <option key={c.id} value={c.id}>
                {c.name}
              </option>
            ))}
        </SelectField>

        <SelectField label="Merchant (optional)" value={merchantId} onChange={(e) => setMerchantId(e.target.value)}>
          <option value="">Unknown merchant</option>
          {merchants.map((m) => (
            <option key={m.id} value={m.id}>
              {m.canonicalName}
            </option>
          ))}
        </SelectField>

        <div className="modal-actions">
          <Button type="button" variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button type="submit" disabled={submitting}>
            {submitting ? 'Saving…' : 'Save changes'}
          </Button>
        </div>
      </form>
    </Modal>
  )
}
