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
  onToggleSidebar: () => void
  onOpenPalette: () => void
  onOpenDrawer: () => void
  onToggleNotifications: () => void
  onProfile: () => void
}

export function Topbar({
  user,
  role,
  pathname,
  unread,
  onToggleSidebar,
  onOpenPalette,
  onOpenDrawer,
  onToggleNotifications,
  onProfile,
}: TopbarProps) {
  const toast = useToast()
  const navigate = useNavigate()
  const crumbs = breadcrumbsFor(pathname, role)

  return (
    <header className="flex h-14 shrink-0 items-center gap-2.5 border-b border-line bg-surface px-1 shadow-[0_1px_2px_rgb(0_0_0/0.04)] md:h-[60px] md:px-4">
      {/* Móvil */}
      <IconButton label="Abrir menú" className="md:hidden" onClick={onOpenDrawer}>
        <Icon name="menu" size={20} />
      </IconButton>
      <div className="flex-1 font-semibold md:hidden">Servicios UNMSM</div>
      <IconButton label="Buscar" className="md:hidden" onClick={onOpenPalette}>
        <Icon name="search" size={20} />
      </IconButton>

      {/* Escritorio */}
      <IconButton label="Colapsar menú lateral" className="hidden md:inline-flex" onClick={onToggleSidebar}>
        <Icon name="panel" />
      </IconButton>
      <nav aria-label="Ruta" className="hidden items-center gap-1.5 whitespace-nowrap text-[13px] text-muted md:flex">
        {crumbs.map((c, i) => (
          <span key={`${c}-${i}`} className="flex items-center gap-1.5">
            {i > 0 && <span className="text-line">/</span>}
            <span className={cn(i === crumbs.length - 1 && 'font-semibold text-fg')}>{c}</span>
          </span>
        ))}
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

      <button
        type="button"
        aria-label="Notificaciones"
        aria-haspopup="dialog"
        onClick={onToggleNotifications}
        className="relative grid size-11 place-items-center rounded-lg border-0 bg-transparent text-fg hover:bg-surface2 md:size-9"
      >
        <Icon name="bell" size={20} />
        {unread > 0 && (
          <span className="absolute right-0.5 top-0.5 grid h-4 min-w-4 place-items-center rounded-full bg-primary px-1 text-[10px] font-bold text-on-primary">
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
        aria-label="Menú de perfil"
        aria-haspopup="menu"
        onClick={onProfile}
        className="rounded-full border-0 bg-transparent p-0"
      >
        <Avatar name={user.name} tone="primary" />
      </button>
      {/* Enlace oculto para saltar al contenido con teclado */}
      <Link to="#main" className="sr-only focus:not-sr-only">
        Saltar al contenido
      </Link>
    </header>
  )
}
