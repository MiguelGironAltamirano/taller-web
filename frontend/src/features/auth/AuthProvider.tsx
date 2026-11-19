import { useQueryClient } from '@tanstack/react-query'
import { useCallback, useEffect, useMemo, useState, useSyncExternalStore, type ReactNode } from 'react'
import { apiPost, refreshSession } from '../../shared/api/client'
import { toSessionUser } from '../../shared/api/mappers'
import type { PasskeyOptions, TokenResponse } from '../../shared/api/types'
import { getPasskey, type RequestOptionsJSON } from '../../shared/lib/webauthn'
import { sessionStore } from '../../shared/session/session-store'
import { AuthContext, type AuthStatus } from './context'

/**
 * Sesión: el token de acceso vive solo en memoria (`sessionStore`) y se recupera al cargar con la
 * cookie `httpOnly` de renovación (spec §3.3). El cliente HTTP renueva y reintenta ante un 401;
 * si la renovación falla, la sesión se vacía y la interfaz vuelve al login.
 */
export function AuthProvider({ children }: { children: ReactNode }) {
  const queryClient = useQueryClient()
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

  const user = useMemo(() => (session ? toSessionUser(session.user) : null), [session])
  const status: AuthStatus = user ? 'authenticated' : ready ? 'anonymous' : 'loading'

  // Al terminar la sesión se descartan los datos del usuario anterior.
  useEffect(() => {
    if (status === 'anonymous') queryClient.clear()
  }, [status, queryClient])

  const login = useCallback(
    async (email: string, password: string) => {
      const token = await apiPost<TokenResponse>('/auth/login', { email: email.trim(), password })
      queryClient.clear()
      sessionStore.set({ accessToken: token.accessToken, user: token.user })
    },
    [queryClient],
  )

  const loginWithPasskey = useCallback(async () => {
    const options = await apiPost<PasskeyOptions<RequestOptionsJSON>>('/auth/passkeys/login/options')
    const credential = await getPasskey(options.publicKey)
    const token = await apiPost<TokenResponse>('/auth/passkeys/login', { challengeId: options.challengeId, credential })
    queryClient.clear()
    sessionStore.set({ accessToken: token.accessToken, user: token.user })
  }, [queryClient])

  const logout = useCallback(async () => {
    try {
      await apiPost('/auth/logout')
    } finally {
      sessionStore.clear()
    }
  }, [])

  const value = useMemo(
    () => ({ status, user, login, loginWithPasskey, logout }),
    [status, user, login, loginWithPasskey, logout],
  )
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
