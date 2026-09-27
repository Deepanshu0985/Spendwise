import { useState } from 'react'
import type { FormEvent } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { ApiRequestError } from '../api/client'
import { authApi } from '../api/auth'
import { Button } from '../components/ui/Button'
import { Field } from '../components/ui/Field'

export function ResetPasswordPage() {
  const [searchParams] = useSearchParams()
  const token = searchParams.get('token') ?? ''
  const [password, setPassword] = useState('')
  const [confirm, setConfirm] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)
  const [done, setDone] = useState(false)

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setError(null)
    if (password !== confirm) {
      setError('Passwords do not match.')
      return
    }
    setSubmitting(true)
    try {
      await authApi.confirmPasswordReset(token, password)
      setDone(true)
    } catch (err) {
      setError(err instanceof ApiRequestError ? err.message : 'Something went wrong. Please try again.')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="auth-shell">
      <div className="auth-brand">
        <div>
          <div className="auth-brand-name">Spendwise</div>
          <div className="auth-brand-tag">Personal Finance Intelligence</div>
        </div>
        <div className="auth-brand-quote">&ldquo;Back in, back on budget.&rdquo;</div>
        <div className="auth-brand-copyright">&copy; {new Date().getFullYear()} Spendwise</div>
      </div>
      <div className="auth-form-panel">
        <div className="auth-form-box">
          {done ? (
            <>
              <div>
                <h1>Password updated</h1>
                <p className="auth-form-subtitle">You can log in with your new password now.</p>
              </div>
              <Button onClick={() => (window.location.href = '/login')}>Go to log in</Button>
            </>
          ) : !token ? (
            <>
              <div>
                <h1>Invalid link</h1>
                <p className="auth-form-subtitle">This reset link is missing its token. Request a new one.</p>
              </div>
              <div className="auth-form-footer">
                <Link to="/login">Back to log in</Link>
              </div>
            </>
          ) : (
            <>
              <div>
                <h1>Choose a new password</h1>
                <p className="auth-form-subtitle">Make it at least 8 characters.</p>
              </div>
              <form onSubmit={(e) => void handleSubmit(e)}>
                {error && <div className="form-error-banner">{error}</div>}
                <Field
                  label="New password"
                  type="password"
                  autoComplete="new-password"
                  required
                  minLength={8}
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="8+ characters"
                />
                <Field
                  label="Confirm"
                  type="password"
                  autoComplete="new-password"
                  required
                  value={confirm}
                  onChange={(e) => setConfirm(e.target.value)}
                  placeholder="Repeat it"
                />
                <Button type="submit" block disabled={submitting}>
                  {submitting ? 'Saving…' : 'Set new password'}
                </Button>
              </form>
            </>
          )}
        </div>
      </div>
    </div>
  )
}
