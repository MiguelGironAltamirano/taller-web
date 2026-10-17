import { Link, useLocation } from 'react-router'
import type { Role, SessionUser } from '../../shared/domain'
import { cn } from '../../shared/lib/cn'
import { Icon, IconButton } from '../../shared/ui'
import { BOTTOM_NAV, isActivePath } from '../navigation'
import { NavList } from './NavList'

type Counts = { open: number; unread: number }

/** Navegación inferior fija (móvil). */
export function BottomNav({ role, counts, onMenu }: { role: Role; counts: Counts; onMenu: () => void }) {
  const { pathname } = useLocation()
  return (
    <nav aria-label="Navegación inferior" className="flex h-[60px] shrink-0 border-t border-line bg-surface md:hidden">
      {BOTTOM_NAV[role].map((item) => {
        const active = isActivePath(item.to, pathname)
        const count = item.badge ? counts[item.badge] : 0
        return (
          <Link
            key={item.to}
            to={item.to}
            aria-current={active ? 'page' : undefined}
            className={cn(
              'relative flex flex-1 flex-col items-center justify-center gap-[3px] text-[11px] no-underline hover:no-underline',
              active ? 'font-bold text-primary-fg' : 'font-medium text-muted',
            )}
          >
            <Icon name={item.icon} size={22} />
            {item.label}
            {count > 0 && (
              <span className="absolute left-[56%] top-1.5 grid h-4 min-w-4 place-items-center rounded-full bg-primary px-1 text-[10px] font-bold text-on-primary">
                {count}
              </span>
            )}
          </Link>
        )
      })}
      <button
        type="button"
        onClick={onMenu}
        className="flex flex-1 flex-col items-center justify-center gap-[3px] border-0 bg-transparent text-[11px] font-medium text-muted"
      >
        <Icon name="menu" size={22} />
        Menú
      </button>
    </nav>
  )
}

type DrawerProps = {
  role: Role
  user: SessionUser
  counts: Counts
  dark: boolean
  onClose: () => void
  onChangePassword: () => void
  onToggleTheme: () => void
  onLogout: () => void
}

/** Cajón lateral con el menú completo (móvil). */
export function MobileDrawer({ role, counts, dark, onClose, onChangePassword, onToggleTheme, onLogout }: DrawerProps) {
  const rowClass = 'h-12 rounded-lg border-0 bg-transparent px-3 text-left text-fg'
  return (
    <>
      <div className="fixed inset-0 z-50 bg-black/45" onClick={onClose} aria-hidden="true" />
      <aside
        aria-label="Menú"
        className="fixed inset-y-0 left-0 z-[51] flex w-[280px] max-w-[86%] animate-pop flex-col bg-surface"
      >
        <div className="flex h-[60px] items-center gap-2.5 border-b border-line px-4">
          <div className="grid size-8 place-items-center rounded-lg bg-primary font-bold text-on-primary">U</div>
          <b className="flex-1">Servicios UNMSM</b>
          <IconButton label="Cerrar" onClick={onClose}>
            <Icon name="x" />
          </IconButton>
        </div>
        <div className="flex flex-1 flex-col gap-0.5 overflow-auto px-3 py-2">
          <NavList role={role} counts={counts} large onNavigate={onClose} />
          <div className="mt-2 flex flex-col gap-0.5 border-t border-line pt-2">
            <button type="button" className={rowClass} onClick={onChangePassword}>
              Cambiar contraseña
            </button>
            <button type="button" className={rowClass} onClick={onToggleTheme}>
              {dark ? 'Modo claro' : 'Modo oscuro'}
            </button>
            <button type="button" className={cn(rowClass, 'text-err-fg')} onClick={onLogout}>
              Cerrar sesión
            </button>
          </div>
        </div>
      </aside>
    </>
  )
}
