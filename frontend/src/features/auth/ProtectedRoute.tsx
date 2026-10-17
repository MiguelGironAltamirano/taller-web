import { Navigate, Outlet } from 'react-router'
import type { Role } from '../../shared/api/types'
import { useAuth } from './auth-context'
import { homePathFor } from './home-path'

export function ProtectedRoute({ roles }: { roles?: Role[] }) {
  const { user, ready } = useAuth()
  if (!ready) return <p className="p-6 text-slate-600">Cargando…</p>
  if (!user) return <Navigate to="/login" replace />
  if (roles && !roles.includes(user.role)) {
    return (
      <p role="alert" className="rounded-md bg-amber-50 p-4 text-amber-900">
        No tienes permiso para ver esta página
      </p>
    )
  }
  return <Outlet />
}

export function HomeRedirect() {
  const { user, ready } = useAuth()
  if (!ready) return <p className="p-6 text-slate-600">Cargando…</p>
  return <Navigate to={user ? homePathFor(user.role) : '/login'} replace />
}
