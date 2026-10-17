import type { ComponentProps, ReactNode } from 'react'
import { cn } from '../../lib/cn'

/** Tarjeta contenedora del diseño: borde fino y radio de 12 px. */
export function Card({ className, ...rest }: ComponentProps<'section'>) {
  return <section className={cn('rounded-xl border border-line bg-surface', className)} {...rest} />
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

type KpiCardProps = {
  label: string
  value: ReactNode
  valueClassName?: string
  onClick?: () => void
  children?: ReactNode
}

/** Tarjeta de indicador; si recibe `onClick` se comporta como botón. */
export function KpiCard({ label, value, valueClassName, onClick, children }: KpiCardProps) {
  const body = (
    <>
      <div className="text-xs text-muted">{label}</div>
      <div className={cn('mt-1 text-[28px] font-bold leading-tight', valueClassName)}>{value}</div>
      {children}
    </>
  )
  const base = 'rounded-xl border border-line bg-surface px-4 py-3.5 text-left'
  return onClick ? (
    <button type="button" onClick={onClick} className={cn(base, 'text-fg hover:border-muted')}>
      {body}
    </button>
  ) : (
    <div className={base}>{body}</div>
  )
}
