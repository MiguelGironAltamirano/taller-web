import type { ComponentProps, ReactNode } from 'react'
import { cn } from '../../lib/cn'
import { Icon, type IconName } from '../atoms/Icon'

/** Tarjeta contenedora del diseño: borde fino, radio de 12 px y sombra sutil. */
export function Card({ className, ...rest }: ComponentProps<'section'>) {
  return <section className={cn('rounded-xl border border-line bg-surface shadow-sm', className)} {...rest} />
}

export function CardTitle({ className, ...rest }: ComponentProps<'h2'>) {
  return <h2 className={cn('mb-3 text-[15px] font-semibold', className)} {...rest} />
}

type PageHeaderProps = { title: string; subtitle?: ReactNode; actions?: ReactNode }

export function PageHeader({ title, subtitle, actions }: PageHeaderProps) {
  return (
    <div className="flex flex-wrap items-center gap-3">
      <div className="min-w-48 flex-1">
        <h1 className="m-0 text-xl font-semibold md:text-2xl">{title}</h1>
        {subtitle && <div className="mt-0.5 text-muted">{subtitle}</div>}
      </div>
      {actions}
    </div>
  )
}

type Trend = { text: string; tone: 'good' | 'bad' | 'neutral'; direction?: 'up' | 'down' }

type KpiCardProps = {
  label: string
  value: ReactNode
  valueClassName?: string
  icon?: IconName
  /** Colores del recuadro del ícono (p. ej. `bg-err-bg text-err-fg`). */
  iconClassName?: string
  trend?: Trend
  onClick?: () => void
  children?: ReactNode
}

const TREND_TONE: Record<Trend['tone'], string> = {
  good: 'bg-ok-bg text-ok-fg',
  bad: 'bg-err-bg text-err-fg',
  neutral: 'bg-surface2 text-muted',
}

/** Tarjeta de indicador; si recibe `onClick` se comporta como botón, si no es un grupo con nombre. */
export function KpiCard({ label, value, valueClassName, icon, iconClassName, trend, onClick, children }: KpiCardProps) {
  const body = (
    <>
      <div className="flex items-start justify-between gap-3">
        <span className="text-[13px] font-medium text-muted">{label}</span>
        {icon && (
          <span className={cn('grid size-8 shrink-0 place-items-center rounded-lg', iconClassName ?? 'bg-surface2 text-muted')}>
            <Icon name={icon} size={16} strokeWidth={2} />
          </span>
        )}
      </div>
      <div className={cn('mt-1.5 text-[28px] font-semibold leading-none tracking-tight tabular-nums', valueClassName)}>
        {value}
      </div>
      {trend && (
        <span
          className={cn(
            'mt-2.5 inline-flex items-center gap-1 self-start rounded-md px-1.5 py-0.5 text-[11.5px] font-semibold',
            TREND_TONE[trend.tone],
          )}
        >
          {trend.direction && <span aria-hidden="true">{trend.direction === 'up' ? '↑' : '↓'}</span>}
          {trend.text}
        </span>
      )}
      {children}
    </>
  )
  const base = 'flex min-w-0 flex-col rounded-xl border border-line bg-surface p-4 text-left shadow-sm'
  return onClick ? (
    <button
      type="button"
      onClick={onClick}
      className={cn(base, 'text-fg transition-[border-color,box-shadow] hover:border-line-strong hover:shadow-md')}
    >
      {body}
    </button>
  ) : (
    <div role="group" aria-label={label} className={base}>
      {body}
    </div>
  )
}
