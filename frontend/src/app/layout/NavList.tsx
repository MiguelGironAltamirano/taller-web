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
                i > 0 && <div className="mx-2 my-2 h-px bg-line" />
              ) : (
                <div className="px-3 pb-1 pt-3 text-[11px] font-semibold uppercase tracking-[.06em] text-muted">
                  {item.section}
                </div>
              ))}
            <Link
              to={item.to}
              title={item.label}
              aria-current={active ? 'page' : undefined}
              onClick={onNavigate}
              className={cn(
                'relative flex items-center gap-2.5 rounded-lg text-fg no-underline hover:bg-surface2 hover:no-underline',
                large ? 'h-12 gap-3 px-3' : 'h-[38px] text-sm',
                collapsed ? 'justify-center px-0' : large ? '' : 'px-3',
                active ? 'bg-primary-soft font-bold text-primary-fg' : 'font-medium',
              )}
            >
              {!large && (
                <span
                  aria-hidden="true"
                  className={cn(
                    'absolute bottom-2 top-2 w-[3px] rounded-r-[3px]',
                    collapsed ? '-left-2' : '-left-3',
                    active ? 'bg-primary' : 'bg-transparent',
                  )}
                />
              )}
              <Icon name={item.icon} size={large ? 20 : 18} className="shrink-0" />
              {!collapsed && <span className="flex-1 whitespace-nowrap">{item.label}</span>}
              {count > 0 && !collapsed && <CountBadge value={count} variant={item.badge === 'open' ? 'neutral' : 'primary'} />}
              {count > 0 && collapsed && <span className="absolute right-1.5 top-1.5 size-2 rounded-full bg-primary" />}
            </Link>
          </div>
        )
      })}
    </>
  )
}
