import { REQUEST_STATES } from '../../../shared/domain'
import { cn } from '../../../shared/lib/cn'
import { OVERDUE_STATUS } from '../lib/filters'

type Counts = { total: number; overdue: number; byState: Record<string, number> }
type Props = { counts: Counts; value: string; onChange: (status: string) => void }

/** Franja de resumen: contadores por estado que también filtran la bandeja. */
export function StatusStrip({ counts, value, onChange }: Props) {
  const items = [
    { id: '', label: 'Todas', n: counts.total },
    ...REQUEST_STATES.map((s) => ({ id: s as string, label: s as string, n: counts.byState[s] })),
    { id: OVERDUE_STATUS, label: 'Vencidas', n: counts.overdue },
  ]
  return (
    <div className="grid grid-cols-[repeat(auto-fit,minmax(96px,1fr))] gap-2 md:grid-cols-[repeat(auto-fit,minmax(120px,1fr))]">
      {items.map((it) => (
        <button
          key={it.id || 'all'}
          type="button"
          aria-pressed={value === it.id}
          onClick={() => onChange(it.id)}
          className={cn(
            'rounded-[10px] border px-3 py-2 text-left text-fg',
            value === it.id ? 'border-primary bg-primary-soft' : 'border-line bg-surface',
          )}
        >
          <div className="text-xs text-muted">{it.label}</div>
          <div className="text-lg font-bold">{it.n}</div>
        </button>
      ))}
    </div>
  )
}
