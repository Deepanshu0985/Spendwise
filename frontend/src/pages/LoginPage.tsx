import { useState } from 'react'
import type { FormEvent } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { ApiRequestError } from '../api/client'
import { useAuth } from '../auth/AuthContext'
import { Button } from '../components/ui/Button'
import { Field } from '../components/ui/Field'

export function LoginPage() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setError(null)
    setSubmitting(true)
    try {
      await login({ email, password })
      const from = (location.state as { from?: { pathname: string } } | null)?.from
      navigate(from?.pathname ?? '/', { replace: true })
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
        <div className="auth-brand-quote">
          &ldquo;Know exactly where every rupee goes, without opening a spreadsheet.&rdquo;
        </div>
        <div className="auth-brand-copyright">&copy; {new Date().getFullYear()} Spendwise</div>
      </div>
      <div className="auth-form-panel">
        <div className="auth-form-box">
          <div>
            <h1>Welcome back</h1>
            <p className="auth-form-subtitle">Log in to see where your money went.</p>
          </div>
          <form onSubmit={(e) => void handleSubmit(e)}>
            {error && <div className="form-error-banner">{error}</div>}
            <Field
              label="Email"
              type="email"
              autoComplete="email"
              required
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="you@example.com"
            />
            <div className="field">
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'baseline' }}>
                <label htmlFor="password">Password</label>
                <Link to="/forgot-password" style={{ fontSize: 12 }}>
                  Forgot password?
                </Link>
              </div>
              <input
                id="password"
                type="password"
                autoComplete="current-password"
                required
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="••••••••"
              />
            </div>
            <Button type="submit" block disabled={submitting}>
              {submitting ? 'Logging in…' : 'Log in'}
            </Button>
          </form>
          <div className="auth-form-footer">
            Don&rsquo;t have an account? <Link to="/register">Create one</Link>
          </div>
        </div>
      </div>
    </div>
  )
}
