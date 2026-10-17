import type { Role, SessionUser } from '../../shared/domain'
import { ROLE_LABELS } from '../../shared/domain'
import { cn } from '../../shared/lib/cn'
import { Avatar } from '../../shared/ui'
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
        collapsed ? 'w-[72px]' : 'w-[248px]',
      )}
    >
      <div className={cn('flex h-[60px] shrink-0 items-center gap-2.5 border-b border-line', collapsed ? 'px-5' : 'px-4')}>
        <div className="grid size-8 shrink-0 place-items-center rounded-lg bg-primary font-bold text-on-primary">U</div>
        {!collapsed && (
          <div className="whitespace-nowrap leading-tight">
            <div className="text-sm font-semibold">UNMSM</div>
            <div className="text-xs text-muted">Servicios Universitarios</div>
          </div>
        )}
      </div>
      <nav aria-label="Principal" className={cn('flex flex-1 flex-col gap-0.5 overflow-auto py-3', collapsed ? 'px-2' : 'px-3')}>
        <NavList role={role} counts={counts} collapsed={collapsed} />
      </nav>
      <div className={cn('shrink-0 border-t border-line py-3', collapsed ? 'px-2' : 'px-3')}>
        <button
          type="button"
          onClick={onProfile}
          aria-haspopup="menu"
          className={cn(
            'flex w-full items-center gap-2.5 rounded-[10px] border-0 bg-surface2 p-2 text-left text-fg',
            collapsed && 'justify-center',
          )}
        >
          <Avatar name={user.name} tone="primary" />
          {!collapsed && (
            <span className="min-w-0 flex-1 leading-tight">
              <span className="block truncate text-[13px] font-semibold">{user.name}</span>
              <span className="block text-xs text-muted">{ROLE_LABELS[role]}</span>
            </span>
          )}
        </button>
      </div>
    </aside>
  )
}
