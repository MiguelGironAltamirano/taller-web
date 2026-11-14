import type { Role, SessionUser } from '../../shared/domain'
import { ROLE_LABELS } from '../../shared/domain'
import { cn } from '../../shared/lib/cn'
import { Avatar, BrandLogo, Icon } from '../../shared/ui'
import { NavList } from './NavList'

type SidebarProps = {
  user: SessionUser
  role: Role
  collapsed: boolean
  counts: { open: number; unread: number }
  onProfile: () => void
}

export function Sidebar({ user, role, collapsed, counts, onProfile }: SidebarProps) {
  return (
    <aside
      className={cn(
        'hidden shrink-0 flex-col overflow-hidden border-r border-line bg-surface transition-[width] duration-200 md:flex',
        collapsed ? 'w-[72px]' : 'w-[252px]',
      )}
    >
      <div className={cn('flex h-[60px] shrink-0 items-center border-b border-line', collapsed ? 'justify-center px-2' : 'px-4')}>
        <BrandLogo compact={collapsed} />
      </div>
      <nav
        aria-label="Principal"
        className={cn('flex flex-1 flex-col gap-0.5 overflow-auto py-3', collapsed ? 'px-2' : 'px-3')}
      >
        <NavList role={role} counts={counts} collapsed={collapsed} />
      </nav>
      <div className={cn('shrink-0 border-t border-line p-3', collapsed && 'px-2')}>
        <button
          type="button"
          onClick={onProfile}
          aria-haspopup="menu"
          className={cn(
            'group flex w-full items-center gap-2.5 rounded-lg border border-transparent bg-transparent p-1.5 text-left text-fg',
            'hover:border-line hover:bg-surface2',
            collapsed && 'justify-center',
          )}
        >
          <Avatar name={user.name} tone="soft" />
          {collapsed && <span className="sr-only">Menú de perfil</span>}
          {!collapsed && (
            <>
              <span className="min-w-0 flex-1 leading-tight">
                <span className="block truncate text-[13px] font-semibold">{user.name}</span>
                <span className="block truncate text-xs text-muted">{ROLE_LABELS[role]}</span>
              </span>
              <Icon name="chevR" size={16} className="-rotate-90 text-muted transition-colors group-hover:text-fg" />
            </>
          )}
        </button>
      </div>
    </aside>
  )
}
