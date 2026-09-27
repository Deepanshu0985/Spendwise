import { createContext, useCallback, useContext, useEffect, useState } from 'react'
import type { ReactNode } from 'react'
import { authApi } from '../api/auth'
import type { LoginRequest, RegisterRequest } from '../api/auth'
import type { UserProfile } from '../api/types'

type AuthStatus = 'checking' | 'authenticated' | 'anonymous'

interface AuthContextValue {
  user: UserProfile | null
  status: AuthStatus
  login: (body: LoginRequest) => Promise<void>
  register: (body: RegisterRequest) => Promise<void>
  logout: () => Promise<void>
}

const AuthContext = createContext<AuthContextValue | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<UserProfile | null>(null)
  const [status, setStatus] = useState<AuthStatus>('checking')

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

  const login = useCallback(async (body: LoginRequest) => {
    const profile = await authApi.login(body)
    setUser(profile)
    setStatus('authenticated')
  }, [])

  // POST /auth/register only creates the account - it never opens a session
  // (see AuthServiceImpl), so a real login call chains right after it.
  const register = useCallback(async (body: RegisterRequest) => {
    await authApi.register(body)
    const profile = await authApi.login({ email: body.email, password: body.password })
    setUser(profile)
    setStatus('authenticated')
  }, [])

  const logout = useCallback(async () => {
    // Always end up logged out client-side, even if the network call itself fails.
    try {
      await authApi.logout()
    } finally {
      setUser(null)
      setStatus('anonymous')
    }
  }, [])

  return <AuthContext.Provider value={{ user, status, login, register, logout }}>{children}</AuthContext.Provider>
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within an AuthProvider')
  return ctx
}
