import { ROLE_LABELS, type SessionUser } from '../../shared/domain'
import { cn } from '../../shared/lib/cn'
import { Icon, type IconName } from '../../shared/ui'

type ProfileMenuProps = {
  user: SessionUser
  dark: boolean
  /** `sidebar`: abre sobre la tarjeta de usuario (abajo a la izquierda); `topbar`: arriba a la derecha. */
  placement: 'sidebar' | 'topbar'
  collapsed: boolean
  onChangePassword: () => void
  onToggleTheme: () => void
  onLogout: () => void
  onClose: () => void
}

type ItemProps = { icon: IconName; label: string; onClick: () => void; danger?: boolean }

function Item({ icon, label, onClick, danger }: ItemProps) {
  return (
    <button
      type="button"
      role="menuitem"
      onClick={onClick}
      className={cn(
        'flex h-9 w-full items-center gap-2 rounded-md border-0 bg-transparent px-2.5 text-left hover:bg-surface2 max-md:h-11',
        danger ? 'text-err-fg' : 'text-fg',
      )}
    >
      <Icon name={icon} size={16} />
      {label}
    </button>
  )
}

export function ProfileMenu({ user, dark, placement, collapsed, onChangePassword, onToggleTheme, onLogout, onClose }: ProfileMenuProps) {
  return (
    <>
      <div className="fixed inset-0 z-40" onClick={onClose} aria-hidden="true" />
      <div
        role="menu"
        aria-label="Menú de perfil"
        className={cn(
          'fixed z-[41] w-[250px] animate-pop rounded-xl border border-line bg-surface p-1.5 shadow-pop',
          placement === 'topbar' ? 'right-2 top-14 md:right-4 md:top-16' : cn('bottom-2', collapsed ? 'left-20' : 'left-3'),
        )}
      >
        <div className="mb-1 border-b border-line px-2.5 py-2">
          <div className="font-semibold">{user.name}</div>
          <div className="text-xs text-muted">
            {ROLE_LABELS[user.role]} · {user.email}
          </div>
        </div>
        <Item icon="lock" label="Cambiar contraseña" onClick={onChangePassword} />
        <Item icon={dark ? 'sun' : 'moon'} label={dark ? 'Modo claro' : 'Modo oscuro'} onClick={onToggleTheme} />
        <Item icon="logout" label="Cerrar sesión" onClick={onLogout} danger />
      </div>
    </>
  )
}
