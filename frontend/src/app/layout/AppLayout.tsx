import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { Outlet, useLocation, useNavigate } from 'react-router'
import { normalizeText } from '../../shared/lib/format'
import { usePersistedFlag } from '../../shared/hooks/usePersistedFlag'
import { useTheme } from '../../shared/hooks/useTheme'
import { ChangePasswordModal, PasskeysModal, consumeWelcome, useAuth, useSessionUser } from '../../features/auth'
import { useToast } from '../../shared/ui'
import { NotificationsPanel, useUnreadCount } from '../../features/notifications'
import { useRequestRows } from '../../features/requests'
import { NAV } from '../navigation'
import { CommandPalette, type PaletteItem } from './CommandPalette'
import { BottomNav, MobileDrawer } from './MobileNav'
import { ProfileMenu } from './ProfileMenu'
import { Sidebar } from './Sidebar'
import { Topbar } from './Topbar'

type Overlay = null | 'profile-sidebar' | 'profile-topbar' | 'notifications' | 'drawer' | 'palette'

/** Estructura de la aplicación autenticada: menú lateral, barra superior, contenido y navegación móvil. */
export function AppLayout() {
  const user = useSessionUser()
  const { logout } = useAuth()
  const navigate = useNavigate()
  const { pathname } = useLocation()
  const { dark, toggle: toggleTheme } = useTheme()
  const [collapsed, setCollapsed] = usePersistedFlag('unmsm_collapsed')
  // El panel abierto se asocia a la ruta en la que se abrió: al navegar deja de aplicar.
  const [opened, setOpened] = useState<{ name: Overlay; path: string }>({ name: null, path: pathname })
  const overlay = opened.path === pathname ? opened.name : null
  const setOverlay = useCallback((name: Overlay) => setOpened({ name, path: pathname }), [pathname])
  const [passwordOpen, setPasswordOpen] = useState(false)
  const [passkeysOpen, setPasskeysOpen] = useState(false)
  const mainRef = useRef<HTMLElement>(null)
  const bellRef = useRef<HTMLButtonElement>(null)

  // La paleta de comandos busca entre las solicitudes ya cargadas (la misma consulta que usa Inicio).
  const rows = useRequestRows().data
  const unread = useUnreadCount()

  // Bienvenida tras un inicio de sesión (no al restaurar la sesión): aviso breve, no bloqueante.
  const toast = useToast()
  useEffect(() => {
    if (!consumeWelcome()) return
    toast('Iniciaste sesión correctamente.', { title: `Hola, ${user.name.split(' ')[0]}` })
  }, [toast, user.name])

  // Cada cambio de ruta vuelve al inicio del contenido.
  useEffect(() => {
    mainRef.current?.scrollTo({ top: 0 })
  }, [pathname])

  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'k') {
        e.preventDefault()
        setOverlay('palette')
      } else if (e.key === 'Escape') {
        setOverlay(null)
        // Al cerrar el panel de la campana, el foco vuelve a ella.
        if (overlay === 'notifications') bellRef.current?.focus()
      }
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [setOverlay, overlay])

  const close = useCallback(() => setOverlay(null), [setOverlay])
  // Cerrar el panel sin navegar (tocar fuera): el foco vuelve a la campana.
  const dismissNotifications = useCallback(() => {
    setOverlay(null)
    bellRef.current?.focus()
  }, [setOverlay])
  const doLogout = async () => {
    setOverlay(null)
    await logout()
    navigate('/login', { replace: true })
  }
  const openPassword = () => {
    setOverlay(null)
    setPasswordOpen(true)
  }
  const openPasskeys = () => {
    setOverlay(null)
    setPasskeysOpen(true)
  }
  const switchTheme = () => {
    setOverlay(null)
    toggleTheme()
  }

  const paletteItems = useMemo<PaletteItem[]>(
    () => [
      ...(user.role === 'solicitante'
        ? [{ label: 'Nueva solicitud', hint: 'Acción', icon: 'plus' as const, run: () => navigate('/solicitudes/nueva') }]
        : []),
      ...NAV[user.role].map((n) => ({ label: `Ir a ${n.label}`, hint: 'Ir a', icon: n.icon, run: () => navigate(n.to) })),
      { label: 'Ir a Notificaciones', hint: 'Ir a', icon: 'bell' as const, run: () => navigate('/notificaciones') },
      { label: 'Cambiar contraseña', hint: 'Cuenta', icon: 'lock' as const, run: () => setPasswordOpen(true) },
      { label: 'Huella y passkeys', hint: 'Cuenta', icon: 'fingerprint' as const, run: () => setPasskeysOpen(true) },
      {
        label: dark ? 'Activar modo claro' : 'Activar modo oscuro',
        hint: 'Cuenta',
        icon: dark ? ('sun' as const) : ('moon' as const),
        run: toggleTheme,
      },
    ],
    [user.role, dark, navigate, toggleTheme],
  )

  const searchRequests = useCallback(
    (query: string): PaletteItem[] => {
      const q = normalizeText(query)
      return (rows ?? [])
        .filter((r) => normalizeText(`${r.code} ${r.description} ${r.category}`).includes(q))
        .slice(0, 5)
        .map((r) => ({
          label: `${r.code} · ${r.category}`,
          hint: r.state,
          icon: 'file' as const,
          run: () => navigate(`/solicitudes/${r.code}`),
        }))
    },
    [rows, navigate],
  )

  return (
    <div className="flex h-dvh bg-bg text-fg">
      <a
        href="#main"
        className="sr-only focus:not-sr-only focus:absolute focus:left-2 focus:top-2 focus:z-[100] focus:rounded-md focus:bg-surface focus:px-3 focus:py-2"
      >
        Saltar al contenido
      </a>
      <Sidebar
        user={user}
        role={user.role}
        collapsed={collapsed}
        onProfile={() => setOverlay(overlay === 'profile-sidebar' ? null : 'profile-sidebar')}
      />
      <div className="flex min-w-0 flex-1 flex-col">
        <Topbar
          user={user}
          role={user.role}
          pathname={pathname}
          unread={unread}
          notificationsOpen={overlay === 'notifications'}
          bellRef={bellRef}
          onToggleSidebar={() => setCollapsed(!collapsed)}
          onOpenPalette={() => setOverlay('palette')}
          onToggleNotifications={() => setOverlay(overlay === 'notifications' ? null : 'notifications')}
          onProfile={() => setOverlay(overlay === 'profile-topbar' ? null : 'profile-topbar')}
        />
        <main id="main" ref={mainRef} className="flex-1 overflow-auto p-3 md:px-6 md:py-5">
          <div className="mx-auto flex max-w-[1440px] animate-rise flex-col gap-3.5">
            <Outlet />
          </div>
        </main>
        <BottomNav role={user.role} onMenu={() => setOverlay('drawer')} />
      </div>

      {(overlay === 'profile-sidebar' || overlay === 'profile-topbar') && (
        <ProfileMenu
          user={user}
          dark={dark}
          placement={overlay === 'profile-sidebar' ? 'sidebar' : 'topbar'}
          collapsed={collapsed}
          onChangePassword={openPassword}
          onPasskeys={openPasskeys}
          onToggleTheme={switchTheme}
          onLogout={doLogout}
          onClose={close}
        />
      )}
      {overlay === 'notifications' && <NotificationsPanel onClose={close} onDismiss={dismissNotifications} />}
      {overlay === 'drawer' && (
        <MobileDrawer
          user={user}
          dark={dark}
          onClose={close}
          onChangePassword={openPassword}
          onPasskeys={openPasskeys}
          onToggleTheme={switchTheme}
          onLogout={doLogout}
        />
      )}
      {overlay === 'palette' && <CommandPalette items={paletteItems} searchRequests={searchRequests} onClose={close} />}
      <ChangePasswordModal open={passwordOpen} onClose={() => setPasswordOpen(false)} />
      <PasskeysModal open={passkeysOpen} onClose={() => setPasskeysOpen(false)} />
    </div>
  )
}
