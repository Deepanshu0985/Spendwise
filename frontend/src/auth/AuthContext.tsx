import { createContext, useCallback, useContext, useEffect, useRef, useState } from 'react'
import type { ReactNode } from 'react'
import { authApi } from '../api/auth'
import type { DeleteAccountRequest, LoginRequest, RegisterRequest } from '../api/auth'
import type { UserProfile } from '../api/types'
import { SplashScreen } from '../components/ui/BrandLoader'
import type { SplashPhase } from '../components/ui/BrandLoader'

type AuthStatus = 'checking' | 'authenticated' | 'anonymous'

interface AuthContextValue {
  user: UserProfile | null
  status: AuthStatus
  login: (body: LoginRequest) => Promise<void>
  register: (body: RegisterRequest) => Promise<void>
  loginWithGoogle: (credential: string) => Promise<void>
  logout: () => Promise<void>
  signOutEverywhere: () => Promise<void>
  deleteAccount: (body: DeleteAccountRequest) => Promise<void>
}

const AuthContext = createContext<AuthContextValue | null>(null)

// Signing in or out waits on the server (a full minute if it was asleep). A branded splash covers that wait instead of
// leaving the page frozen, and stays up for a moment on success so it never just flashes.
const SPLASH_MIN_MS = 900
const SPLASH_FADE_MS = 320

interface Splash {
  phase: SplashPhase
  title: string
  detail?: string
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<UserProfile | null>(null)
  const [status, setStatus] = useState<AuthStatus>('checking')
  const [splash, setSplash] = useState<Splash>({ phase: 'gone', title: '' })
  const hideTimer = useRef<number | undefined>(undefined)

  const withSplash = useCallback(async <T,>(title: string, detail: string, work: () => Promise<T>): Promise<T> => {
    window.clearTimeout(hideTimer.current)
    setSplash({ phase: 'in', title, detail })
    const started = performance.now()
    try {
      const result = await work()
      const remaining = SPLASH_MIN_MS - (performance.now() - started)
      if (remaining > 0) await new Promise((resolve) => window.setTimeout(resolve, remaining))
      return result
    } finally {
      setSplash((current) => ({ ...current, phase: 'out' }))
      hideTimer.current = window.setTimeout(() => setSplash((current) => ({ ...current, phase: 'gone' })), SPLASH_FADE_MS)
    }
  }, [])

  useEffect(() => {
    let cancelled = false
    authApi
      .me()
      .then((profile) => {
        if (cancelled) return
        setUser(profile)
        setStatus('authenticated')
      })
      .catch(() => {
        if (cancelled) return
        setStatus('anonymous')
      })
    return () => {
      cancelled = true
    }
  }, [])

  const login = useCallback(
    (body: LoginRequest) =>
      withSplash('Signing you in', 'Getting your numbers ready', async () => {
        const profile = await authApi.login(body)
        setUser(profile)
        setStatus('authenticated')
      }),
    [withSplash],
  )

  // POST /auth/register only creates the account - it never opens a session
  // (see AuthServiceImpl), so a real login call chains right after it.
  const register = useCallback(
    (body: RegisterRequest) =>
      withSplash('Creating your account', 'Setting up your space', async () => {
        await authApi.register(body)
        const profile = await authApi.login({ email: body.email, password: body.password })
        setUser(profile)
        setStatus('authenticated')
      }),
    [withSplash],
  )

  const loginWithGoogle = useCallback(
    (credential: string) =>
      withSplash('Signing you in', 'Getting your numbers ready', async () => {
        const profile = await authApi.loginWithGoogle(credential)
        setUser(profile)
        setStatus('authenticated')
      }),
    [withSplash],
  )

  const logout = useCallback(
    () =>
      withSplash('Signing you out', 'See you soon', async () => {
        // Always end up logged out client-side, even if the network call itself fails.
        try {
          await authApi.logout()
        } finally {
          setUser(null)
          setStatus('anonymous')
        }
      }),
    [withSplash],
  )

  const signOutEverywhere = useCallback(
    () =>
      withSplash('Signing you out everywhere', 'Ending every session', async () => {
        try {
          await authApi.revokeAllSessions()
        } finally {
          setUser(null)
          setStatus('anonymous')
        }
      }),
    [withSplash],
  )

  // Only after the server has confirmed the erasure does the app forget the person; a wrong password leaves everything as it was.
  const deleteAccount = useCallback(
    (body: DeleteAccountRequest) =>
      withSplash('Deleting your account', 'Removing your data for good', async () => {
        await authApi.deleteAccount(body)
        setUser(null)
        setStatus('anonymous')
      }),
    [withSplash],
  )

  return (
    <AuthContext.Provider value={{ user, status, login, register, loginWithGoogle, logout, signOutEverywhere, deleteAccount }}>
      {children}
      <SplashScreen phase={splash.phase} title={splash.title} detail={splash.detail} />
    </AuthContext.Provider>
  )
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within an AuthProvider')
  return ctx
}
