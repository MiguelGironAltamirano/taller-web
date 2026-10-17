import { createContext, useContext } from 'react'
import type { Role, SessionUser } from '../../shared/domain'

export type AuthStatus = 'loading' | 'authenticated' | 'anonymous'

export type AuthContextValue = {
  status: AuthStatus
  user: SessionUser | null
  login: (email: string, password: string) => Promise<void>
  logout: () => Promise<void>
}

export const AuthContext = createContext<AuthContextValue | null>(null)

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth debe usarse dentro de <AuthProvider>')
  return ctx
}

/** Usuario autenticado; solo se usa dentro de rutas protegidas. */
export function useSessionUser(): SessionUser {
  const { user } = useAuth()
  if (!user) throw new Error('Se requiere una sesión activa')
  return user
}

/** Página de inicio de cada rol (spec §4). */
export function homePathFor(role: Role): string {
  if (role === 'solicitante') return '/inicio'
  if (role === 'tecnico') return '/solicitudes'
  return '/dashboard'
}
