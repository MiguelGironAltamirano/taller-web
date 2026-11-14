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

type PageHeaderProps = {
  title: string
  subtitle?: ReactNode
  actions?: ReactNode
  /** Ícono de la sección (el mismo del menú, para orientar). */
  icon?: IconName
}

/** Recuadro guinda del ícono de sección: mismo degradado que el encabezado del inicio de sesión. */
export function SectionIcon({ name, className }: { name: IconName; className?: string }) {
  return (
    <span
      aria-hidden="true"
      className={cn(
        'grid size-12 shrink-0 place-items-center rounded-xl text-white',
        'shadow-[inset_0_1px_0_rgb(255_255_255/0.18),0_8px_18px_-10px_var(--brand)]',
        className,
      )}
      style={{
        background:
          'radial-gradient(90% 120% at 50% 0%, rgb(255 255 255 / 0.16), transparent 70%),' +
          'linear-gradient(180deg, color-mix(in srgb, var(--brand) 82%, white), var(--brand))',
      }}
    >
      <Icon name={name} size={22} strokeWidth={1.8} />
    </span>
  )
}

/** Encabezado de página: ícono de sección, título (h1) en la serif institucional, subtítulo y acciones. */
export function PageHeader({ title, subtitle, actions, icon }: PageHeaderProps) {
  return (
    <header className="flex flex-wrap items-center gap-x-4 gap-y-3 pb-1">
      <div className="flex min-w-60 flex-1 items-center gap-4">
        {icon && <SectionIcon name={icon} />}
        <div className="min-w-0">
          <h1 className="m-0 font-display text-[24px] font-semibold leading-tight text-fg md:text-[28px]">{title}</h1>
          {subtitle && <div className="mt-1 text-[13.5px] text-muted">{subtitle}</div>}
        </div>
      </div>
      {actions && <div className="flex flex-wrap items-center gap-2">{actions}</div>}
    </header>
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
