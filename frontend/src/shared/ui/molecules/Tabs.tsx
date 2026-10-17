import type { ReactNode } from 'react'
import { cn } from '../../lib/cn'

export type TabItem<T extends string> = { id: T; label: string; count?: number | string }

type TabsProps<T extends string> = {
  items: TabItem<T>[]
  value: T
  onChange: (id: T) => void
  label: string
  /** `underline`: pestañas de página; `pill`: filtros compactos. */
  variant?: 'underline' | 'pill'
}

export function Tabs<T extends string>({ items, value, onChange, label, variant = 'underline' }: TabsProps<T>) {
  if (variant === 'pill') {
    return (
      <div role="tablist" aria-label={label} className="flex gap-1.5">
        {items.map((t) => {
          const on = t.id === value
          return (
            <button
              key={t.id}
              type="button"
              role="tab"
              aria-selected={on}
              onClick={() => onChange(t.id)}
              className={cn(
                'h-8.5 rounded-full border px-3.5 font-medium text-fg',
                on ? 'border-primary bg-primary-soft' : 'border-line bg-surface',
              )}
            >
              {t.label}
            </button>
          )
        })}
      </div>
    )
  }
  return (
    <div role="tablist" aria-label={label} className="flex overflow-x-auto border-b border-line px-2">
      {items.map((t) => {
        const on = t.id === value
        return (
          <button
            key={t.id}
            type="button"
            role="tab"
            aria-selected={on}
            onClick={() => onChange(t.id)}
            className={cn(
              'h-11 whitespace-nowrap border-0 border-b-2 bg-transparent px-3.5',
              on ? 'border-primary font-bold text-primary-fg' : 'border-transparent font-medium text-muted',
            )}
          >
            {t.label}
            {t.count !== undefined && t.count !== '' && <span className="ml-1.5 text-xs text-muted">{t.count}</span>}
          </button>
        )
      })}
    </div>
  )
}

type SegmentedProps<T extends string> = {
  items: { id: T; label: ReactNode }[]
  value: T
  onChange: (id: T) => void
  label: string
}

/** Control segmentado (tabla/kanban, densidad, rango de fechas). */
export function Segmented<T extends string>({ items, value, onChange, label }: SegmentedProps<T>) {
  return (
    <div role="group" aria-label={label} className="flex overflow-hidden rounded-lg border border-line">
      {items.map((it) => (
        <button
          key={it.id}
          type="button"
          aria-pressed={it.id === value}
          onClick={() => onChange(it.id)}
          className={cn(
            'h-[34px] border-0 px-2.5 text-[13px] text-fg max-md:h-11',
            it.id === value ? 'bg-primary-soft' : 'bg-surface',
          )}
        >
          {it.label}
        </button>
      ))}
    </div>
  )
}
