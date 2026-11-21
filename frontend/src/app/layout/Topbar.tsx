import type { Ref } from 'react'
import { Link, useNavigate } from 'react-router'
import type { Role, SessionUser } from '../../shared/domain'
import { cn } from '../../shared/lib/cn'
import { Avatar, Button, Icon, IconButton, useToast } from '../../shared/ui'
import { breadcrumbsFor } from '../navigation'

type TopbarProps = {
  user: SessionUser
  role: Role
  pathname: string
  unread: number
  /** El panel rápido de la campana está abierto. */
  notificationsOpen: boolean
  /** Para devolver el foco a la campana al cerrar el panel. */
  bellRef: Ref<HTMLButtonElement>
  onToggleSidebar: () => void
  onOpenPalette: () => void
  onToggleNotifications: () => void
  onProfile: () => void
}

export function Topbar({
  user,
  role,
  pathname,
  unread,
  notificationsOpen,
  bellRef,
  onToggleSidebar,
  onOpenPalette,
  onToggleNotifications,
  onProfile,
}: TopbarProps) {
  const toast = useToast()
  const navigate = useNavigate()
  const crumbs = breadcrumbsFor(pathname, role)

  return (
    <header className="flex h-14 shrink-0 items-center gap-2.5 border-b border-line bg-surface px-1 shadow-[0_1px_2px_rgb(0_0_0/0.04)] md:h-[60px] md:px-4">
      {/* Móvil */}
      <div className="flex-1 pl-3 font-semibold md:hidden">Servicios UNMSM</div>
      <IconButton label="Buscar" className="md:hidden" onClick={onOpenPalette}>
        <Icon name="search" size={20} />
      </IconButton>

      {/* Escritorio */}
      <IconButton label="Colapsar menú lateral" className="hidden md:inline-flex" onClick={onToggleSidebar}>
        <Icon name="panel" />
      </IconButton>
      <nav aria-label="Ruta" className="hidden min-w-0 md:block">
        <ol className="m-0 flex list-none items-center gap-0.5 whitespace-nowrap p-0 text-[13px]">
          {crumbs.map((c, i) => (
            <li key={`${c.label}-${i}`} className="flex min-w-0 items-center gap-0.5">
              {i > 0 && <Icon name="chevR" size={14} strokeWidth={2} className="shrink-0 text-line-strong" />}
              {c.to ? (
                <Link
                  to={c.to}
                  className="flex items-center gap-1.5 rounded-md px-2 py-1 font-medium text-muted no-underline transition-colors hover:bg-surface2 hover:text-fg hover:no-underline"
                >
                  {i === 0 && <Icon name="home" size={15} strokeWidth={1.9} />}
                  {c.label}
                </Link>
              ) : (
                <span
                  aria-current="page"
                  className={cn(
                    'flex items-center gap-1.5 truncate rounded-md px-2 py-1 font-semibold text-fg',
                    i > 0 && 'bg-surface2',
                  )}
                >
                  {i === 0 && <Icon name="home" size={15} strokeWidth={1.9} />}
                  {c.label}
                </span>
              )}
            </li>
          ))}
        </ol>
      </nav>
      <div className="hidden flex-1 justify-center md:flex">
        <button
          type="button"
          onClick={onOpenPalette}
          className="flex h-9 w-full max-w-[420px] items-center gap-2 rounded-lg border border-line bg-surface2 px-3 text-left text-muted"
        >
          <Icon name="search" size={16} />
          <span className="flex-1">Buscar solicitudes o acciones</span>
          <kbd className="shrink-0 whitespace-nowrap rounded border border-line bg-surface px-1.5 py-0.5 text-[11px]">Ctrl K</kbd>
        </button>
      </div>
      {role === 'solicitante' && (
        <Button variant="primary" className="hidden md:inline-flex" onClick={() => navigate('/solicitudes/nueva')}>
          <Icon name="plus" size={16} strokeWidth={2} />
          Nueva solicitud
        </Button>
      )}

      {/* Names come from content (not aria-label) so the visible badge and avatar never contradict them (WCAG 2.5.3). */}
      <button
        ref={bellRef}
        type="button"
        aria-haspopup="dialog"
        aria-expanded={notificationsOpen}
        onClick={onToggleNotifications}
        className={cn(
          'relative grid size-11 place-items-center rounded-lg border-0 text-fg hover:bg-surface2 md:size-9',
          notificationsOpen || pathname === '/notificaciones' ? 'bg-primary-soft text-primary-fg' : 'bg-transparent',
        )}
      >
        <Icon name="bell" size={20} />
        <span className="sr-only">{unread > 0 ? `Notificaciones, ${unread} sin leer` : 'Notificaciones'}</span>
        {unread > 0 && (
          <span
            aria-hidden="true"
            className="absolute right-0.5 top-0.5 grid h-4 min-w-4 place-items-center rounded-full bg-primary px-1 text-[10px] font-bold text-on-primary"
          >
            {unread}
          </span>
        )}
      </button>
      <IconButton
        label="Ayuda"
        className="hidden md:inline-flex"
        onClick={() => toast('Centro de ayuda: mesa@unmsm.edu.pe')}
      >
        <Icon name="help" />
      </IconButton>
      <button
        type="button"
        aria-haspopup="menu"
        onClick={onProfile}
        className="rounded-full border-0 bg-transparent p-0"
      >
        <Avatar name={user.name} tone="primary" />
        <span className="sr-only">Menú de perfil</span>
      </button>
    </header>
  )
}
