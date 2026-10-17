import type { CSSProperties, ReactNode } from 'react'
import { stateIndex, type Priority, type RequestState } from '../../domain'
import { cn } from '../../lib/cn'

const BASE = 'inline-flex h-[22px] items-center gap-1 whitespace-nowrap rounded-full px-2 text-xs font-semibold'

const PRIORITY_SLUG: Record<Priority, string> = {
  Crítica: 'critica',
  Alta: 'alta',
  Media: 'media',
  Baja: 'baja',
}

function Pill({ style, className, children }: { style: CSSProperties; className?: string; children: ReactNode }) {
  return (
    <span className={cn(BASE, className)} style={style}>
      {children}
    </span>
  )
}

/** Insignia de estado: siempre con texto, nunca solo color. */
export function StatusBadge({ state }: { state: RequestState }) {
  const i = stateIndex(state)
  return <Pill style={{ background: `var(--st-${i}-bg)`, color: `var(--st-${i}-fg)` }}>{state}</Pill>
}

export function PriorityBadge({ priority }: { priority: Priority }) {
  const slug = PRIORITY_SLUG[priority]
  return <Pill style={{ background: `var(--pr-${slug}-bg)`, color: `var(--pr-${slug}-fg)` }}>{priority}</Pill>
}

export function OverdueBadge() {
  return (
    <Pill style={{ background: 'var(--venc-bg)', color: 'var(--venc-fg)' }}>
      <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" aria-hidden="true">
        <path d="M12 21a9 9 0 1 0 0-18 9 9 0 0 0 0 18z M12 7v5l3 2" />
      </svg>
      Vencida
    </Pill>
  )
}

type Tone = 'ok' | 'muted'

/** Insignia genérica (p. ej. Activo / Inactivo). */
export function ToneBadge({ tone, children }: { tone: Tone; children: ReactNode }) {
  return (
    <span className={cn(BASE, tone === 'ok' ? 'bg-ok-bg text-ok-fg' : 'bg-surface2 text-muted')}>{children}</span>
  )
}

/** Contador numérico (menú lateral, campana, navegación inferior). */
export function CountBadge({ value, variant = 'primary' }: { value: number | string; variant?: 'primary' | 'neutral' }) {
  return (
    <span
      className={cn(
        'grid h-5 min-w-5 place-items-center rounded-full px-1.5 text-[11px] font-bold',
        variant === 'primary' ? 'bg-primary text-on-primary' : 'bg-surface2 text-fg',
      )}
    >
      {value}
    </span>
  )
}
