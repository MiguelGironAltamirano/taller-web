import { REQUEST_STATES, type RequestState } from '../../../shared/domain'
import { cn } from '../../../shared/lib/cn'
import { Icon, stateIcon, type IconName } from '../../../shared/ui'
import { OVERDUE_STATUS } from '../lib/filters'

type Counts = { total: number; overdue: number; byState: Record<string, number> }
type Props = { counts: Counts; value: string; onChange: (status: string) => void }

type Item = { id: string; label: string; n: number; icon: IconName; danger?: boolean }

/** Pestañas de estado con contador: resumen de la bandeja que también la filtra. */
export function StatusStrip({ counts, value, onChange }: Props) {
  const items: Item[] = [
    { id: '', label: 'Todas', n: counts.total, icon: 'folder' },
    ...REQUEST_STATES.map((s: RequestState) => ({ id: s, label: s, n: counts.byState[s] ?? 0, icon: stateIcon(s) })),
    { id: OVERDUE_STATUS, label: 'Vencidas', n: counts.overdue, icon: 'clock', danger: true },
  ]
  return (
    <div role="group" aria-label="Filtrar por estado" className="-mb-px flex gap-1 overflow-x-auto px-3 pt-3 [scrollbar-width:none]">
      {items.map((it) => {
        const selected = value === it.id
        return (
          <button
            key={it.id || 'all'}
            type="button"
            aria-pressed={selected}
            onClick={() => onChange(it.id)}
            className={cn(
              'relative inline-flex h-10 shrink-0 items-center gap-2 rounded-t-lg border-0 bg-transparent px-3 text-[13px] font-medium',
              'after:absolute after:inset-x-2 after:bottom-0 after:h-[2px] after:rounded-full after:transition-colors',
              selected
                ? 'text-fg after:bg-primary'
                : 'text-muted after:bg-transparent hover:bg-surface2/70 hover:text-fg',
            )}
          >
            <Icon
              name={it.icon}
              size={15}
              strokeWidth={selected ? 2 : 1.75}
              className={cn(it.danger && it.n > 0 ? 'text-err-fg' : selected ? 'text-primary-fg' : '')}
            />
            {it.label}
            <span
              className={cn(
                'min-w-6 rounded-md px-1.5 py-0.5 text-center text-[11px] font-semibold tabular-nums',
                it.danger && it.n > 0
                  ? 'bg-err-bg text-err-fg'
                  : selected
                    ? 'bg-primary text-on-primary'
                    : 'bg-surface2 text-muted',
              )}
            >
              {it.n}
            </span>
          </button>
        )
      })}
    </div>
  )
}
