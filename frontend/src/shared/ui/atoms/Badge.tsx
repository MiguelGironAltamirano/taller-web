import type { CSSProperties, ReactNode } from 'react'
import { stateIndex, type Priority, type RequestState } from '../../domain'
import { cn } from '../../lib/cn'
import { Icon } from './Icon'
import { stateIcon } from './state-icon'

const BASE =
  'inline-flex h-6 items-center gap-1.5 whitespace-nowrap rounded-md px-2 text-xs font-medium leading-none shadow-[inset_0_0_0_1px_color-mix(in_srgb,currentColor_18%,transparent)]'

const PRIORITY_SLUG: Record<Priority, string> = {
  Crítica: 'critica',
  Alta: 'alta',
  Media: 'media',
  Baja: 'baja',
}

const PRIORITY_LEVEL: Record<Priority, number> = { Crítica: 4, Alta: 3, Media: 2, Baja: 1 }

function Pill({ style, className, children }: { style?: CSSProperties; className?: string; children: ReactNode }) {
  return (
    <span className={cn(BASE, className)} style={style}>
      {children}
    </span>
  )
}

/** Insignia de estado: ícono + texto, nunca solo color. */
export function StatusBadge({ state }: { state: RequestState }) {
  const i = stateIndex(state)
  return (
    <Pill style={{ background: `var(--st-${i}-bg)`, color: `var(--st-${i}-fg)` }}>
      <Icon name={stateIcon(state)} size={13} strokeWidth={2} className="shrink-0" />
      {state}
    </Pill>
  )
}

/** Barras de nivel (1–3); el nivel máximo se dibuja como alerta. */
function PriorityGlyph({ level }: { level: number }) {
  if (level >= 4) {
    return (
      <svg width="13" height="13" viewBox="0 0 16 16" aria-hidden="true" className="shrink-0">
        <rect x="1" y="1" width="14" height="14" rx="3.5" fill="currentColor" />
        <path d="M8 4.5v4.2" stroke="var(--surface)" strokeWidth="1.8" strokeLinecap="round" />
        <circle cx="8" cy="11.3" r="1.05" fill="var(--surface)" />
      </svg>
    )
  }
  return (
    <svg width="13" height="13" viewBox="0 0 16 16" aria-hidden="true" className="shrink-0">
      {[0, 1, 2].map((b) => (
        <rect
          key={b}
          x={1.5 + b * 5}
          y={10 - b * 3.5}
          width="3"
          height={4.5 + b * 3.5}
          rx="1"
          fill="currentColor"
          opacity={b < level ? 1 : 0.28}
        />
      ))}
    </svg>
  )
}

export function PriorityBadge({ priority }: { priority: Priority }) {
  const slug = PRIORITY_SLUG[priority]
  return (
    <Pill style={{ background: `var(--pr-${slug}-bg)`, color: `var(--pr-${slug}-fg)` }}>
      <PriorityGlyph level={PRIORITY_LEVEL[priority] ?? 2} />
      {priority}
    </Pill>
  )
}

export function OverdueBadge() {
  return (
    <Pill style={{ background: 'var(--venc-bg)', color: 'var(--venc-fg)' }}>
      <Icon name="clock" size={13} strokeWidth={2} className="shrink-0" />
      Vencida
    </Pill>
  )
}

type Tone = 'ok' | 'muted'

/** Insignia genérica (p. ej. Activo / Inactivo). */
export function ToneBadge({ tone, children }: { tone: Tone; children: ReactNode }) {
  return <Pill className={tone === 'ok' ? 'bg-ok-bg text-ok-fg' : 'bg-surface2 text-muted'}>{children}</Pill>
}

/** Contador numérico (menú lateral, campana, navegación inferior). */
export function CountBadge({ value, variant = 'primary' }: { value: number | string; variant?: 'primary' | 'neutral' }) {
  return (
    <span
      className={cn(
        'grid h-5 min-w-5 place-items-center rounded-full px-1.5 text-[11px] font-bold tabular-nums',
        variant === 'primary' ? 'bg-primary text-on-primary' : 'bg-surface2 text-fg',
      )}
    >
      {value}
    </span>
  )
}
