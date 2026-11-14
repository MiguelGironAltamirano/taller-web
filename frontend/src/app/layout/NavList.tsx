import { Link, useLocation } from 'react-router'
import type { Role } from '../../shared/domain'
import { cn } from '../../shared/lib/cn'
import { CountBadge, Icon } from '../../shared/ui'
import { isActivePath, NAV } from '../navigation'

type Counts = { open: number; unread: number }

type NavListProps = {
  role: Role
  counts: Counts
  /** Menú lateral colapsado: solo íconos con tooltip. */
  collapsed?: boolean
  /** Variante del cajón móvil: filas altas (48 px). */
  large?: boolean
  onNavigate?: () => void
}

/** Lista de navegación por rol, con secciones, ítem activo y contadores. */
export function NavList({ role, counts, collapsed = false, large = false, onNavigate }: NavListProps) {
  const { pathname } = useLocation()
  const items = NAV[role]
  return (
    <>
      {items.map((item, i) => {
        const active = isActivePath(item.to, pathname)
        const count = item.badge ? counts[item.badge] : 0
        return (
          <div key={item.to} className="contents">
            {item.section &&
              (collapsed ? (
                i > 0 && <div className="mx-3 my-2.5 h-px bg-line" />
              ) : (
                <div className={cn('px-3 pb-1.5 text-[11px] font-medium text-muted', i > 0 ? 'pt-5' : 'pt-2')}>
                  {item.section}
                </div>
              ))}
            <Link
              to={item.to}
              title={collapsed ? item.label : undefined}
              aria-current={active ? 'page' : undefined}
              onClick={onNavigate}
              className={cn(
                'group relative flex items-center gap-2.5 rounded-lg no-underline transition-colors hover:no-underline',
                large ? 'h-12 gap-3 px-3' : 'h-9 text-[13.5px]',
                collapsed ? 'mx-auto w-11 justify-center px-0' : large ? '' : 'px-2.5',
                active
                  ? 'bg-primary-soft font-semibold text-primary-fg'
                  : 'font-medium text-muted hover:bg-surface2 hover:text-fg',
              )}
            >
              <Icon
                name={item.icon}
                size={large ? 20 : 18}
                strokeWidth={active ? 2 : 1.75}
                className={cn('shrink-0', active ? 'text-primary-fg' : 'text-muted group-hover:text-fg')}
              />
              {!collapsed && <span className="flex-1 truncate">{item.label}</span>}
              {count > 0 && !collapsed && <CountBadge value={count} variant={item.badge === 'open' ? 'neutral' : 'primary'} />}
              {count > 0 && collapsed && (
                <span className="absolute right-1 top-1 size-2 rounded-full bg-primary ring-2 ring-surface" />
              )}
            </Link>
          </div>
        )
      })}
    </>
  )
}
