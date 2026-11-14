import type { ReactNode } from 'react'
import { cn } from '../../lib/cn'
import { Button } from './Button'
import { Icon, type IconName } from './Icon'

/** Esqueleto de carga. */
export function Skeleton({ className }: { className?: string }) {
  return <div className={cn('animate-pulse-soft rounded-md bg-surface2', className)} aria-hidden="true" />
}

export function SkeletonRows({ count = 5, rowClass = 'h-12' }: { count?: number; rowClass?: string }) {
  return (
    <div className="flex flex-col gap-2.5 p-3" role="status" aria-label="Cargando">
      {Array.from({ length: count }, (_, i) => (
        <Skeleton key={i} className={rowClass} />
      ))}
    </div>
  )
}

type EmptyStateProps = {
  icon?: IconName
  title: string
  description?: string
  action?: ReactNode
  /** Variante compacta: no ocupa una pantalla completa. */
  compact?: boolean
}

export function EmptyState({ icon = 'file', title, description, action, compact }: EmptyStateProps) {
  return (
    <div className={cn('flex flex-col items-center gap-2 px-4 text-center', compact ? 'py-6' : 'py-8')}>
      <div className="grid size-12 place-items-center rounded-xl bg-surface2 text-muted">
        <Icon name={icon} size={24} />
      </div>
      <div className="font-semibold">{title}</div>
      {description && <div className="max-w-sm text-muted">{description}</div>}
      {action}
    </div>
  )
}

type ErrorStateProps = {
  title: string
  description?: string
  /** Vuelve a pedir los datos; el botón "Reintentar" muestra que está cargando mientras `retrying`. */
  onRetry: () => void
  retrying?: boolean
  compact?: boolean
}

/** Estado de error de una carga: explica qué falló y siempre ofrece reintentar (plan de Lita §5). */
export function ErrorState({ title, description, onRetry, retrying = false, compact }: ErrorStateProps) {
  return (
    <div role="alert" className={cn('flex flex-col items-center gap-2 px-4 text-center', compact ? 'py-6' : 'py-8')}>
      <div className="grid size-12 place-items-center rounded-xl bg-err-bg text-err-fg">
        <Icon name="alert" size={24} />
      </div>
      <div className="font-semibold">{title}</div>
      {description && <div className="max-w-sm text-muted">{description}</div>}
      <Button onClick={onRetry} loading={retrying} className="mt-1">
        Reintentar
      </Button>
    </div>
  )
}

type AlertTone = 'error' | 'info' | 'success' | 'warn'

const ALERT_TONE: Record<AlertTone, string> = {
  error: 'bg-err-bg text-err-fg',
  info: 'bg-info-bg text-info-fg',
  success: 'bg-ok-bg text-ok-fg',
  warn: 'bg-warn-bg text-warn-fg',
}

const ALERT_ICON: Record<AlertTone, string> = {
  error: 'M12 9v4m0 4h.01 M12 21a9 9 0 1 0 0-18 9 9 0 0 0 0 18z',
  warn: 'M12 9v4m0 4h.01 M10.3 3.6 1.4 18a2 2 0 0 0 1.7 3h17.8a2 2 0 0 0 1.7-3L13.7 3.6a2 2 0 0 0-3.4 0z',
  info: 'M12 16v-4m0-4h.01 M12 21a9 9 0 1 0 0-18 9 9 0 0 0 0 18z',
  success: 'M20 6 9 17l-5-5',
}

export function Alert({ tone = 'error', children, className }: { tone?: AlertTone; children: ReactNode; className?: string }) {
  return (
    <div role={tone === 'error' ? 'alert' : 'status'} className={cn('flex items-start gap-2 rounded-lg px-3 py-2.5 text-[13px]', ALERT_TONE[tone], className)}>
      <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true" className="mt-px shrink-0">
        <path d={ALERT_ICON[tone]} />
      </svg>
      <div>{children}</div>
    </div>
  )
}

type ProgressBarProps = { value: number; color?: string; label?: string; className?: string }

/** Barra de progreso (SLA, carga). El color se pasa como valor CSS (token o hex de gráfico). */
export function ProgressBar({ value, color = 'var(--primary)', label, className }: ProgressBarProps) {
  const pct = Math.min(100, Math.max(0, value))
  return (
    <div
      role="progressbar"
      aria-valuenow={Math.round(pct)}
      aria-valuemin={0}
      aria-valuemax={100}
      aria-label={label}
      className={cn('h-1.5 overflow-hidden rounded-full bg-line/70', className)}
    >
      <div
        className="h-full rounded-full transition-[width] duration-700 ease-out"
        style={{ width: `${pct}%`, background: color }}
      />
    </div>
  )
}
