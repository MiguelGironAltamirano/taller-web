import { Icon } from '../../../shared/ui'
import { OVERDUE_STATUS, VIEW_LABELS, type ListFilters, type ServerFilters } from '../lib/filters'

type Props = {
  filters: ListFilters
  server: ServerFilters
  areas: { id: number; name: string }[]
  onChange: (patch: Partial<ListFilters>) => void
  onServerChange: (patch: Partial<ServerFilters>) => void
  onClear: () => void
}

/** Chips removibles con los filtros activos y "Limpiar filtros". */
export function FilterChips({ filters, server, areas, onChange, onServerChange, onClear }: Props) {
  const chips: { label: string; remove: () => void }[] = []
  if (filters.status) {
    chips.push({
      label: `Estado: ${filters.status === OVERDUE_STATUS ? 'Vencidas' : filters.status}`,
      remove: () => onChange({ status: '' }),
    })
  }
  if (filters.category) chips.push({ label: `Categoría: ${filters.category}`, remove: () => onChange({ category: '' }) })
  if (filters.q) chips.push({ label: `Texto: ${filters.q}`, remove: () => onChange({ q: '' }) })
  if (filters.view !== 'all') chips.push({ label: `Vista: ${VIEW_LABELS[filters.view]}`, remove: () => onChange({ view: 'all' }) })
  if (filters.priority) chips.push({ label: `Prioridad: ${filters.priority}`, remove: () => onChange({ priority: '' }) })
  if (filters.technician) chips.push({ label: `Técnico: ${filters.technician}`, remove: () => onChange({ technician: '' }) })
  if (server.areaId) {
    const name = areas.find((a) => String(a.id) === server.areaId)?.name ?? server.areaId
    chips.push({ label: `Área: ${name}`, remove: () => onServerChange({ areaId: '' }) })
  }
  if (server.from) chips.push({ label: `Desde: ${server.from}`, remove: () => onServerChange({ from: '' }) })
  if (server.to) chips.push({ label: `Hasta: ${server.to}`, remove: () => onServerChange({ to: '' }) })

  if (chips.length === 0) return null
  return (
    <div className="flex flex-wrap items-center gap-1.5">
      {chips.map((c) => (
        <button
          key={c.label}
          type="button"
          aria-label={`Quitar filtro ${c.label}`}
          onClick={c.remove}
          className="inline-flex h-7 items-center gap-1.5 rounded-full border border-line bg-primary-soft px-2.5 text-xs font-medium text-primary-fg"
        >
          {c.label}
          <Icon name="x" size={12} strokeWidth={2} />
        </button>
      ))}
      <button type="button" onClick={onClear} className="border-0 bg-transparent text-xs text-muted underline">
        Limpiar filtros
      </button>
    </div>
  )
}
