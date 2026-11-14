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
  /** Etiqueta corta sobre el título: la sección o el contexto de la página. */
  eyebrow?: string
  /** `inline`: ícono a la izquierda (páginas); `stacked`: título grande en bloque (formularios de acceso). */
  layout?: 'inline' | 'stacked'
  /** Parte final del título resaltada en guinda (p. ej. title="Iniciar" titleAccent="sesión"). */
  titleAccent?: string
}

function HeaderIcon({ name, large }: { name: IconName; large?: boolean }) {
  return (
    <span
      className={cn(
        'relative grid shrink-0 place-items-center overflow-hidden bg-surface text-primary-fg ring-1 ring-inset ring-line',
        'shadow-[0_1px_2px_rgb(0_0_0/0.05),0_10px_24px_-14px_var(--primary)]',
        large ? 'size-14 rounded-[18px]' : 'size-12 rounded-2xl',
      )}
    >
      <span aria-hidden="true" className="absolute inset-0 bg-[linear-gradient(160deg,var(--primary-soft)_10%,transparent_75%)]" />
      <Icon name={name} size={large ? 26 : 22} strokeWidth={1.9} className="relative" />
    </span>
  )
}

const EYEBROW = 'm-0 text-[11px] font-semibold uppercase leading-none tracking-[0.14em] text-primary-fg'

/** Encabezado de página: ícono de sección, etiqueta, título (h1), subtítulo y acciones. */
export function PageHeader({ title, titleAccent, subtitle, actions, icon, eyebrow, layout = 'inline' }: PageHeaderProps) {
  const heading = (
    <>
      {title}
      {titleAccent && (
        <>
          {' '}
          <span className="text-primary-fg">{titleAccent}</span>
        </>
      )}
    </>
  )
  if (layout === 'stacked') {
    return (
      <header className="flex flex-col items-start gap-5">
        {icon && <HeaderIcon name={icon} large />}
        <div>
          {eyebrow && (
            <p className={cn(EYEBROW, 'mb-4 flex items-center gap-2.5 tracking-[0.2em]')}>
              <span aria-hidden="true" className="h-[2px] w-6 rounded-full bg-primary" />
              {eyebrow}
            </p>
          )}
          <h1 className="m-0 text-[42px] font-extrabold leading-[1.02] tracking-[-0.035em] text-fg">{heading}</h1>
          {subtitle && <div className="mt-3.5 text-[15px] leading-relaxed text-muted">{subtitle}</div>}
        </div>
        {actions}
      </header>
    )
  }
  return (
    <header className="flex flex-wrap items-center gap-x-4 gap-y-3 pb-1">
      <div className="flex min-w-60 flex-1 items-center gap-4">
        {icon && <HeaderIcon name={icon} />}
        <div className="min-w-0">
          {eyebrow && <p className={cn(EYEBROW, 'mb-1')}>{eyebrow}</p>}
          <h1 className="m-0 text-[22px] font-bold leading-tight tracking-[-0.015em] text-fg md:text-[27px]">{heading}</h1>
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
