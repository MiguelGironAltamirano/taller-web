import { useCallback, useEffect, useMemo, useState, useSyncExternalStore, type ReactNode } from 'react'
import { apiPost, refreshSession } from '../../shared/api/client'
import type { LoginBody, TokenResponse } from '../../shared/api/types'
import { sessionStore } from '../../shared/session/session-store'
import { AuthContext, type AuthContextValue } from './auth-context'

export function AuthProvider({ children }: { children: ReactNode }) {
  const session = useSyncExternalStore(sessionStore.subscribe, sessionStore.get)
  const [ready, setReady] = useState(false)

  useEffect(() => {
    let active = true
    void refreshSession().then(() => {
      if (active) setReady(true)
    })
    return () => {
      active = false
    }
  }, [])

  const login = useCallback(async (body: LoginBody) => {
    const token = await apiPost<TokenResponse>('/auth/login', body)
    sessionStore.set({ accessToken: token.accessToken, user: token.user })
    return token.user
  }, [])

  const logout = useCallback(async () => {
    try {
      await apiPost<void>('/auth/logout')
    } finally {
      sessionStore.clear()
    }
  }, [])

  const value = useMemo<AuthContextValue>(
    () => ({ user: session?.user ?? null, ready, login, logout }),
    [session, ready, login, logout],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
