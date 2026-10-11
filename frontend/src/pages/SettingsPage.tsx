import { useState } from 'react'
import type { FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { ApiRequestError } from '../api/client'
import { authApi } from '../api/auth'
import { useAuth } from '../auth/AuthContext'
import { GoogleSignIn } from '../components/auth/GoogleSignIn'
import { Button } from '../components/ui/Button'
import { Field } from '../components/ui/Field'
import { Modal } from '../components/ui/Modal'

function signInMethods(hasPassword: boolean, googleLinked: boolean): string {
  const methods = [hasPassword ? 'Email and password' : null, googleLinked ? 'Google' : null].filter(Boolean)
  return methods.length > 0 ? methods.join(' and ') : 'Email and password'
}

export function SettingsPage() {
  const { user, signOutEverywhere } = useAuth()
  const [exporting, setExporting] = useState(false)
  const [exportError, setExportError] = useState<string | null>(null)
  const [signingOut, setSigningOut] = useState(false)
  const [deleting, setDeleting] = useState(false)

  if (!user) return null

  async function downloadData() {
    setExportError(null)
    setExporting(true)
    try {
      const blob = await authApi.exportData()
      const url = URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = `spendwise-data-${new Date().toISOString().slice(0, 10)}.json`
      document.body.appendChild(link)
      link.click()
      link.remove()
      URL.revokeObjectURL(url)
    } catch (err) {
      setExportError(err instanceof ApiRequestError ? err.message : 'Could not download your data. Please try again.')
    } finally {
      setExporting(false)
    }
  }

  async function endAllSessions() {
    setSigningOut(true)
    try {
      await signOutEverywhere()
    } finally {
      setSigningOut(false)
    }
  }

  return (
    <section>
      <div className="page-header">
        <div>
          <h1>Settings</h1>
          <div className="page-subtitle">Your account, your data, and how to take it with you.</div>
        </div>
      </div>

      <div className="settings-stack">
        <div className="card">
          <div className="card-title">Account</div>
          <dl className="settings-facts">
            <div>
              <dt>Name</dt>
              <dd>{user.fullName}</dd>
            </div>
            <div>
              <dt>Email</dt>
              <dd>{user.email}</dd>
            </div>
            <div>
              <dt>Sign-in</dt>
              <dd>{signInMethods(user.hasPassword, user.googleLinked)}</dd>
            </div>
          </dl>
        </div>

        <div className="card">
          <div className="card-title">Your data</div>
          <p className="settings-text">
            Download everything Spendwise holds about you - accounts, transactions, budgets, goals, statements and insights - as
            one file. It never includes your password.
          </p>
          {exportError && <div className="form-error-banner">{exportError}</div>}
          <Button variant="secondary" onClick={() => void downloadData()} disabled={exporting}>
            {exporting ? 'Preparing your file…' : 'Download my data'}
          </Button>
        </div>

        <div className="card">
          <div className="card-title">Privacy</div>
          <p className="settings-text">
            What Spendwise holds, who handles it, and your rights are written up in the <Link to="/privacy">privacy policy</Link>.
          </p>
        </div>

        <div className="card">
          <div className="card-title">Sessions</div>
          <p className="settings-text">Signed in on a device you no longer use? End every session, including this one.</p>
          <Button variant="secondary" onClick={() => void endAllSessions()} disabled={signingOut}>
            Sign out everywhere
          </Button>
        </div>

        <div className="card settings-danger">
          <div className="card-title">Delete account</div>
          <p className="settings-text">
            This erases your account and everything in it - transactions, accounts, budgets, goals, statements and the files you
            uploaded - immediately and for good. It cannot be undone. Download your data first if you want a copy.
          </p>
          <Button variant="danger" onClick={() => setDeleting(true)}>
            Delete my account…
          </Button>
        </div>
      </div>

      {deleting && <DeleteAccountModal onClose={() => setDeleting(false)} />}
    </section>
  )
}

function DeleteAccountModal({ onClose }: { onClose: () => void }) {
  const { user, deleteAccount } = useAuth()
  const [password, setPassword] = useState('')
  const [googleCredential, setGoogleCredential] = useState<string | null>(null)
  const [confirmation, setConfirmation] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  const needsPassword = user?.hasPassword ?? true
  const proofGiven = needsPassword ? password.length > 0 : googleCredential !== null
  const ready = confirmation.trim() === 'DELETE' && proofGiven && !submitting

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    if (!ready) return
    setError(null)
    setSubmitting(true)
    try {
      await deleteAccount({
        confirmation: confirmation.trim(),
        ...(needsPassword ? { password } : { googleCredential: googleCredential ?? undefined }),
      })
    } catch (err) {
      if (err instanceof ApiRequestError) {
        const reason = err.fieldError('password') ?? err.fieldError('googleCredential')
        setError(reason ? `Your ${needsPassword ? 'password' : 'Google sign-in'} ${reason}.` : err.message)
      } else {
        setError('Something went wrong. Nothing was deleted - please try again.')
      }
      setSubmitting(false)
    }
  }

  return (
    <Modal title="Delete your account" onClose={onClose}>
      <form onSubmit={(e) => void handleSubmit(e)} className="settings-delete-form">
        <p className="settings-text">
          Everything will be erased now, for good. To be sure it is you, {needsPassword ? 'enter your password' : 'confirm with Google'} and type
          DELETE.
        </p>
        {error && <div className="form-error-banner">{error}</div>}
        {needsPassword ? (
          <Field
            label="Password"
            type="password"
            autoComplete="current-password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
          />
        ) : googleCredential ? (
          <div className="settings-confirmed">Google sign-in confirmed.</div>
        ) : (
          <GoogleSignIn withDivider={false} onCredential={setGoogleCredential} />
        )}
        <Field
          label="Type DELETE to confirm"
          autoComplete="off"
          value={confirmation}
          onChange={(e) => setConfirmation(e.target.value)}
        />
        <div className="modal-actions">
          <Button type="button" variant="secondary" onClick={onClose} disabled={submitting}>
            Cancel
          </Button>
          <Button type="submit" variant="danger" disabled={!ready}>
            {submitting ? 'Deleting…' : 'Delete everything'}
          </Button>
        </div>
      </form>
    </Modal>
  )
}
