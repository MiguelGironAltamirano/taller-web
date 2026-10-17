import { Navigate, Outlet, useLocation } from 'react-router'
import type { Role } from '../../shared/domain'
import { Skeleton } from '../../shared/ui'
import { homePathFor, useAuth } from './context'
import { ErrorPage } from './ErrorPage'

function Splash() {
  return (
    <div className="grid min-h-screen place-items-center" role="status" aria-label="Cargando sesión">
      <Skeleton className="size-12 rounded-xl" />
    </div>
  )
}

/** Exige sesión; si no hay, redirige al login recordando la ruta pedida. */
export function RequireAuth() {
  const { status } = useAuth()
  const location = useLocation()
  if (status === 'loading') return <Splash />
  if (status === 'anonymous') return <Navigate to="/login" replace state={{ from: location.pathname }} />
  return <Outlet />
}

/** Exige uno de los roles; en caso contrario muestra el 403 dentro del layout. */
export function RequireRole({ roles }: { roles: Role[] }) {
  const { user } = useAuth()
  if (!user || !roles.includes(user.role)) return <ErrorPage kind="403" />
  return <Outlet />
}

/** Pantallas públicas (login/registro): si ya hay sesión, va a la página de inicio del rol. */
export function PublicOnly() {
  const { status, user } = useAuth()
  if (status === 'loading') return <Splash />
  if (status === 'authenticated' && user) return <Navigate to={homePathFor(user.role)} replace />
  return <Outlet />
}

/** `/` → inicio según el rol. */
export function RoleHome() {
  const { user } = useAuth()
  return <Navigate to={user ? homePathFor(user.role) : '/login'} replace />
}
