import { PRIORITIES } from '../../../shared/domain'
import { Icon, Segmented, Select } from '../../../shared/ui'
import { VIEW_LABELS, type ListFilters, type ListSort, type ListView } from '../lib/filters'

type Props = {
  filters: ListFilters
  categories: string[]
  /** Solo el supervisor filtra por técnico. */
  technicians: string[] | null
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
export function ListToolbar({ filters, categories, technicians, canKanban, mode, density, onChange, onMode, onDensity }: Props) {
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
          placeholder="Buscar por código o texto"
          className="h-9 w-full rounded-lg border border-line bg-surface pl-[34px] pr-2.5 max-md:h-11"
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
          <option key={c} value={c}>
            {c}
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
        {PRIORITIES.map((p) => (
          <option key={p} value={p}>
            {p}
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
            <option key={t} value={t}>
              {t}
            </option>
          ))}
        </Select>
      )}
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
