import type { ReactNode } from 'react'
import { cn } from '../../lib/cn'
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

type AlertTone = 'error' | 'info' | 'success'
const ALERT_TONE: Record<AlertTone, string> = {
  error: 'bg-err-bg text-err-fg',
  info: 'bg-info-bg text-info-fg',
  success: 'bg-ok-bg text-ok-fg',
}

export function Alert({ tone = 'error', children, className }: { tone?: AlertTone; children: ReactNode; className?: string }) {
  return (
    <div role={tone === 'error' ? 'alert' : 'status'} className={cn('flex gap-2 rounded-lg px-3 py-2.5 text-[13px]', ALERT_TONE[tone], className)}>
      {children}
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
      className={cn('h-1.5 overflow-hidden rounded-full bg-surface2', className)}
    >
      <div className="h-full" style={{ width: `${pct}%`, background: color }} />
    </div>
  )
}
