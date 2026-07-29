import { createContext, ReactNode, useCallback, useContext, useEffect, useMemo, useState } from 'react'
import {
  authStore,
  login as loginRequest,
  logout as logoutRequest,
  Role,
  Session,
  SESSION_EXPIRED_EVENT,
} from './api'

type AuthContextValue = {
  session: Session | null
  role: Role | null
  sessionExpired: boolean
  signIn: (email: string, password: string) => Promise<Session>
  signOut: () => Promise<void>
  dismissSessionExpired: () => void
}

const AuthContext = createContext<AuthContextValue | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [session, setSession] = useState<Session | null>(() => authStore.get())
  const [sessionExpired, setSessionExpired] = useState(false)

  useEffect(() => {
    const expire = () => {
      setSession(null)
      setSessionExpired(true)
    }
    window.addEventListener(SESSION_EXPIRED_EVENT, expire)
    return () => window.removeEventListener(SESSION_EXPIRED_EVENT, expire)
  }, [])

  const signIn = useCallback(async (email: string, password: string) => {
    const next = await loginRequest(email, password)
    setSession(next)
    setSessionExpired(false)
    return next
  }, [])

  const signOut = useCallback(async () => {
    await logoutRequest()
    setSession(null)
  }, [])

  const value = useMemo<AuthContextValue>(
    () => ({
      session,
      role: session?.user.role ?? null,
      sessionExpired,
      signIn,
      signOut,
      dismissSessionExpired: () => setSessionExpired(false),
    }),
    [session, sessionExpired, signIn, signOut],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth(): AuthContextValue {
  const value = useContext(AuthContext)
  if (!value) throw new Error('useAuth deve ser usado dentro de AuthProvider')
  return value
}

export function homeForRole(role: Role): string {
  if (role === 'EMPLOYEE') return '/meu/dashboard'
  if (role === 'ADMIN') return '/admin/dashboard'
  return '/equipe/dashboard'
}
