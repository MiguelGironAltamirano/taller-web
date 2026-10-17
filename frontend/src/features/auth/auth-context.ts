import { createContext, useContext } from 'react'
import type { LoginBody, UserSummary } from '../../shared/api/types'

export type AuthContextValue = {
  user: UserSummary | null
  /** False until the first attempt to restore the session has finished. */
  ready: boolean
  login: (body: LoginBody) => Promise<UserSummary>
  logout: () => Promise<void>
}

export const AuthContext = createContext<AuthContextValue | null>(null)

export function useAuth(): AuthContextValue {
  const value = useContext(AuthContext)
  if (!value) throw new Error('useAuth must be used inside AuthProvider')
  return value
}
