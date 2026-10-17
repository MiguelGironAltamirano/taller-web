import { useQuery } from '@tanstack/react-query'
import { NavLink, Outlet, useNavigate } from 'react-router'
import { useAuth } from '../features/auth/auth-context'
import { apiGet } from '../shared/api/client'

type SystemInfo = { name: string; version: string }

const linkClass = ({ isActive }: { isActive: boolean }) =>
  `rounded px-3 py-1.5 text-sm font-medium ${isActive ? 'bg-slate-900 text-white' : 'text-slate-700 hover:bg-slate-200'}`

export function AppLayout() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  const info = useQuery({
    queryKey: ['system-info'],
    queryFn: () => apiGet<SystemInfo>('/system/info'),
  })

  let status = 'Conectando…'
  if (info.isSuccess) status = `Versión ${info.data.version}`
  if (info.isError) status = 'API no disponible'

  async function handleLogout() {
    try {
      await logout()
    } finally {
      void navigate('/login', { replace: true })
    }
  }

  return (
    <div className="flex min-h-screen flex-col bg-slate-50">
      <header className="border-b border-slate-200 bg-white">
        <div className="mx-auto flex max-w-5xl flex-wrap items-center gap-x-6 gap-y-2 px-4 py-3">
          <span className="text-lg font-bold text-slate-900">Servicios Universitarios</span>
          <nav aria-label="Principal" className="flex gap-2">
            {user?.role === 'SOLICITANTE' && (
              <NavLink to="/solicitudes" className={linkClass}>
                Mis solicitudes
              </NavLink>
            )}
            {user && user.role !== 'SOLICITANTE' && (
              <NavLink to="/bandeja" className={linkClass}>
                Bandeja
              </NavLink>
            )}
          </nav>
          <div className="ml-auto flex items-center gap-3 text-sm">
            <span className="text-slate-700">{user?.fullName}</span>
            <button
              type="button"
              onClick={() => void handleLogout()}
              className="rounded border border-slate-300 px-3 py-1.5 font-medium text-slate-700 hover:bg-slate-100"
            >
              Cerrar sesión
            </button>
          </div>
        </div>
      </header>
      <main className="mx-auto w-full max-w-5xl flex-1 px-4 py-6">
        <Outlet />
      </main>
      <footer className="border-t border-slate-200 px-4 py-3 text-center text-xs text-slate-500">{status}</footer>
    </div>
  )
}
