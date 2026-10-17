import { useQueryClient } from '@tanstack/react-query'
import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react'
import { apiPost, ApiError, setAccessToken, setUnauthorizedHandler } from '../../shared/api/client'
import { toSessionUser } from '../../shared/api/mappers'
import type { TokenResponse } from '../../shared/api/types'
import type { SessionUser } from '../../shared/domain'
import { AuthContext, type AuthStatus } from './context'

/**
 * Sesión: el token de acceso vive solo en memoria y se recupera al cargar con `/auth/refresh`,
 * que usa la cookie `httpOnly` de renovación (spec §3.3).
 */
export function AuthProvider({ children }: { children: ReactNode }) {
  const queryClient = useQueryClient()
  const [status, setStatus] = useState<AuthStatus>('loading')
  const [user, setUser] = useState<SessionUser | null>(null)

  const start = useCallback((res: TokenResponse) => {
    setAccessToken(res.accessToken)
    setUser(toSessionUser(res.user))
    setStatus('authenticated')
  }, [])

  const clear = useCallback(() => {
    setAccessToken(null)
    setUser(null)
    setStatus('anonymous')
    queryClient.clear()
  }, [queryClient])

  useEffect(() => {
    let active = true
    apiPost<TokenResponse>('/auth/refresh')
      .then((res) => active && start(res))
      .catch((e: unknown) => {
        if (active && (e instanceof ApiError || e instanceof TypeError)) clear()
      })
    return () => {
      active = false
    }
  }, [start, clear])

  useEffect(() => {
    setUnauthorizedHandler(clear)
    return () => setUnauthorizedHandler(null)
  }, [clear])

  const login = useCallback(
    async (email: string, password: string) => {
      const res = await apiPost<TokenResponse>('/auth/login', { email: email.trim(), password })
      queryClient.clear()
      start(res)
    },
    [queryClient, start],
  )

  const logout = useCallback(async () => {
    try {
      await apiPost('/auth/logout')
    } finally {
      clear()
    }
  }, [clear])

  const value = useMemo(() => ({ status, user, login, logout }), [status, user, login, logout])
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
