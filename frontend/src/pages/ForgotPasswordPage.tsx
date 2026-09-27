import { useState } from 'react'
import type { FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { authApi } from '../api/auth'
import { Button } from '../components/ui/Button'
import { Field } from '../components/ui/Field'

export function ForgotPasswordPage() {
  const [email, setEmail] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [sent, setSent] = useState(false)

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setSubmitting(true)
    try {
      // Always shows the same confirmation regardless of whether the email
      // exists (authentication-api.md) - the backend never discloses that.
      await authApi.requestPasswordReset(email)
    } finally {
      setSubmitting(false)
      setSent(true)
    }
  }

  return (
    <div className="auth-shell">
      <div className="auth-brand">
        <div>
          <div className="auth-brand-name">Spendwise</div>
          <div className="auth-brand-tag">Personal Finance Intelligence</div>
        </div>
        <div className="auth-brand-quote">&ldquo;A forgotten password shouldn&rsquo;t mean a forgotten budget.&rdquo;</div>
        <div className="auth-brand-copyright">&copy; {new Date().getFullYear()} Spendwise</div>
      </div>
      <div className="auth-form-panel">
        <div className="auth-form-box">
          {sent ? (
            <>
              <div>
                <h1>Check your email</h1>
                <p className="auth-form-subtitle">
                  If an account exists for <strong>{email}</strong>, we&rsquo;ve sent a link to reset your password.
                </p>
              </div>
              <div className="auth-form-footer">
                <Link to="/login">Back to log in</Link>
              </div>
            </>
          ) : (
            <>
              <div>
                <h1>Reset your password</h1>
                <p className="auth-form-subtitle">We&rsquo;ll email you a link to choose a new one.</p>
              </div>
              <form onSubmit={(e) => void handleSubmit(e)}>
                <Field
                  label="Email"
                  type="email"
                  autoComplete="email"
                  required
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder="you@example.com"
                />
                <Button type="submit" block disabled={submitting}>
                  {submitting ? 'Sending…' : 'Send reset link'}
                </Button>
              </form>
              <div className="auth-form-footer">
                <Link to="/login">Back to log in</Link>
              </div>
            </>
          )}
        </div>
      </div>
    </div>
  )
}
