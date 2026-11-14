import { Icon, Segmented, Select } from '../../../shared/ui'
import { VIEW_LABELS, type ListFilters, type ListSort, type ListView } from '../lib/filters'

/** Opción de un filtro: `value` es el id que se envía al servidor y `label` lo que se muestra. */
export type FilterOption = { value: string; label: string }

type Props = {
  filters: ListFilters
  categories: FilterOption[]
  priorities: FilterOption[]
  /** Solo el supervisor filtra por técnico. */
  technicians: FilterOption[] | null
  /** Solo el administrador filtra por área; los demás roles ya ven una sola. */
  areas: FilterOption[] | null
  /** Rol con tablero kanban (técnico, supervisor, administrador). */
  canKanban: boolean
  mode: 'table' | 'kanban'
  density: 'comfy' | 'compact'
  onChange: (patch: Partial<ListFilters>) => void
  onMode: (mode: 'table' | 'kanban') => void
  onDensity: (d: 'comfy' | 'compact') => void
}

const SORT_LABELS: Record<ListSort, string> = { due: 'Vencimiento', new: 'Más recientes', prio: 'Prioridad' }

/** Barra de herramientas del listado: búsqueda, filtros, vistas guardadas, orden, modo y densidad. */
export function ListToolbar({
  filters,
  categories,
  priorities,
  technicians,
  areas,
  canKanban,
  mode,
  density,
  onChange,
  onMode,
  onDensity,
}: Props) {
  return (
    <div className="flex flex-wrap items-center gap-2">
      <div className="relative min-w-0 flex-[1_1_220px]">
        <span className="pointer-events-none absolute left-2.5 top-[9px] text-muted">
          <Icon name="search" size={16} />
        </span>
        <input
          type="search"
          aria-label="Buscar"
          value={filters.q}
          onChange={(e) => onChange({ q: e.target.value })}
          placeholder="Buscar por código, texto, solicitante o ambiente"
          className="h-9 w-full rounded-lg border border-line bg-surface pl-[34px] pr-2.5 text-fg placeholder:text-muted transition-[border-color,box-shadow] hover:border-line-strong focus:border-primary focus:outline-none focus:ring-2 focus:ring-primary/20 max-md:h-11"
        />
      </div>
      <Select
        aria-label="Categoría"
        value={filters.category}
        onChange={(e) => onChange({ category: e.target.value })}
        className="h-9 w-auto max-w-full max-md:h-11"
      >
        <option value="">Todas las categorías</option>
        {categories.map((c) => (
          <option key={c.value} value={c.value}>
            {c.label}
          </option>
        ))}
      </Select>
      <Select
        aria-label="Vistas guardadas"
        value={filters.view}
        onChange={(e) => onChange({ view: e.target.value as ListView })}
        className="h-9 w-auto max-md:h-11"
      >
        {(Object.keys(VIEW_LABELS) as ListView[]).map((v) => (
          <option key={v} value={v}>
            Vista: {VIEW_LABELS[v]}
          </option>
        ))}
      </Select>
      <Select
        aria-label="Ordenar"
        value={filters.sort}
        onChange={(e) => onChange({ sort: e.target.value as ListSort })}
        className="h-9 w-auto max-md:h-11"
      >
        {(Object.keys(SORT_LABELS) as ListSort[]).map((s) => (
          <option key={s} value={s}>
            Orden: {SORT_LABELS[s]}
          </option>
        ))}
      </Select>
      <Select
        aria-label="Prioridad"
        value={filters.priority}
        onChange={(e) => onChange({ priority: e.target.value })}
        className="h-9 w-auto max-md:h-11"
      >
        <option value="">Toda prioridad</option>
        {priorities.map((p) => (
          <option key={p.value} value={p.value}>
            {p.label}
          </option>
        ))}
      </Select>
      {technicians && (
        <Select
          aria-label="Técnico"
          value={filters.technician}
          onChange={(e) => onChange({ technician: e.target.value })}
          className="h-9 w-auto max-md:h-11"
        >
          <option value="">Todos los técnicos</option>
          {technicians.map((t) => (
            <option key={t.value} value={t.value}>
              {t.label}
            </option>
          ))}
        </Select>
      )}
      {areas && (
        <Select
          aria-label="Área"
          value={filters.area}
          onChange={(e) => onChange({ area: e.target.value })}
          className="h-9 w-auto max-md:h-11"
        >
          <option value="">Todas las áreas</option>
          {areas.map((a) => (
            <option key={a.value} value={a.value}>
              {a.label}
            </option>
          ))}
        </Select>
      )}
      <label className="flex items-center gap-1.5 text-xs text-muted">
        Desde
        <input
          type="date"
          value={filters.from}
          max={filters.to || undefined}
          onChange={(e) => onChange({ from: e.target.value })}
          className="h-9 rounded-lg border border-line bg-surface px-2 text-sm text-fg max-md:h-11"
        />
      </label>
      <label className="flex items-center gap-1.5 text-xs text-muted">
        Hasta
        <input
          type="date"
          value={filters.to}
          min={filters.from || undefined}
          onChange={(e) => onChange({ to: e.target.value })}
          className="h-9 rounded-lg border border-line bg-surface px-2 text-sm text-fg max-md:h-11"
        />
      </label>
      <span className="hidden flex-1 md:block" />
      {canKanban && (
        <Segmented
          label="Vista"
          value={mode}
          onChange={onMode}
          items={[
            { id: 'table', label: 'Tabla' },
            { id: 'kanban', label: 'Kanban' },
          ]}
        />
      )}
      <div className="hidden md:block">
        <Segmented
          label="Densidad"
          value={density}
          onChange={onDensity}
          items={[
            { id: 'comfy', label: 'Cómoda' },
            { id: 'compact', label: 'Compacta' },
          ]}
        />
      </div>
    </div>
  )
}
