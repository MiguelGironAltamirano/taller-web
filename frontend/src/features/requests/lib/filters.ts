import { REQUEST_STATES, type RequestState } from '../../../shared/domain'
import { STATUS_ORDER, stateToStatus, statusToState } from '../../../shared/api/mappers'
import type { RequestCounts, RequestListQuery, RequestSortParam, RequestStatus } from '../../../shared/api/types'
import { buildCsv } from '../../../shared/lib/csv'
import { dateLong } from '../../../shared/lib/format'
import type { RowView } from './view'

export type ListView = 'all' | 'attention' | 'venc' | 'open' | 'confirm' | 'critsa'
export type ListSort = 'due' | 'new' | 'prio'

/**
 * Estado de los filtros de la bandeja tal como vive en la URL. `category`, `priority`, `technician` y `area`
 * guardan el id (como texto) que espera el servidor; vacío significa "todos". `from` y `to` son días
 * `AAAA-MM-DD` de la fecha de registro (inclusive, en hora de Lima, como los interpreta el servidor).
 */
export type ListFilters = {
  q: string
  category: string
  priority: string
  technician: string
  area: string
  from: string
  to: string
  /** Un estado, `__venc` (vencidas) o vacío. */
  status: string
  view: ListView
  sort: ListSort
}

export const VIEW_LABELS: Record<ListView, string> = {
  all: 'Todas',
  attention: 'Requieren mi atención',
  venc: 'Mis vencidas',
  open: 'Abiertas',
  confirm: 'Por confirmar',
  critsa: 'Críticas sin asignar',
}

export const OVERDUE_STATUS = '__venc'

const OPEN_STATUSES: RequestStatus[] = ['REGISTRADA', 'EN_EVALUACION', 'ASIGNADA', 'EN_ATENCION']

/** Estados en los que una solicitud puede requerir la atención del usuario (una cerrada nunca la requiere). */
const ATTENTION_STATUSES: RequestStatus[] = [...OPEN_STATUSES, 'RESUELTA']

const DAY = /^\d{4}-\d{2}-\d{2}$/

const SORT_PARAM: Record<ListSort, RequestSortParam> = { due: 'due', new: 'newest', prio: 'priority' }

/**
 * Traducción de los filtros de la interfaz a `GET /requests`:
 * - `query`: parámetros listos (sin paginación).
 * - `empty`: la combinación no puede devolver nada (p. ej. "Resuelta" con la vista "Abiertas"); no se consulta.
 * - `pending`: falta un dato del catálogo (el id de la prioridad crítica) para construir la consulta.
 */
export type ServerPlan = { kind: 'query'; query: RequestListQuery } | { kind: 'empty' } | { kind: 'pending' }

const positiveId = (value: string): number | undefined => {
  const id = Number(value)
  return Number.isInteger(id) && id > 0 ? id : undefined
}

export function toServerQuery(f: ListFilters, criticalPriorityId: number | undefined): ServerPlan {
  // `null` = cualquier estado; cada restricción se intersecta con las anteriores.
  const allowed: { statuses: RequestStatus[] | null } = { statuses: null }
  const narrow = (only: RequestStatus[]) => {
    allowed.statuses = allowed.statuses === null ? only : allowed.statuses.filter((s) => only.includes(s))
  }
  const query: RequestListQuery = { sort: SORT_PARAM[f.sort] }
  const q = f.q.trim()
  if (q) query.q = q
  query.categoryId = positiveId(f.category)
  query.priorityId = positiveId(f.priority)
  query.technicianId = positiveId(f.technician)
  query.areaId = positiveId(f.area)
  if (DAY.test(f.from)) query.from = f.from
  if (DAY.test(f.to)) query.to = f.to
  if (query.from && query.to && query.to < query.from) return { kind: 'empty' }

  if (f.status === OVERDUE_STATUS) query.overdue = true
  else if ((REQUEST_STATES as readonly string[]).includes(f.status)) narrow([stateToStatus(f.status as RequestState)])

  switch (f.view) {
    case 'attention':
      // Depende de las notificaciones sin leer (dato del cliente): el servidor acota y la bandeja refina.
      narrow(ATTENTION_STATUSES)
      break
    case 'venc':
      query.overdue = true
      break
    case 'open':
      narrow(OPEN_STATUSES)
      break
    case 'confirm':
      narrow(['RESUELTA'])
      break
    case 'critsa':
      if (criticalPriorityId === undefined) return { kind: 'pending' }
      if (query.priorityId !== undefined && query.priorityId !== criticalPriorityId) return { kind: 'empty' }
      if (query.technicianId !== undefined) return { kind: 'empty' }
      query.priorityId = criticalPriorityId
      query.unassigned = true
      narrow(OPEN_STATUSES)
      break
    default:
      break
  }
  if (allowed.statuses !== null) {
    if (allowed.statuses.length === 0) return { kind: 'empty' }
    query.status = allowed.statuses
  }
  return { kind: 'query', query: Object.fromEntries(Object.entries(query).filter(([, v]) => v !== undefined)) as RequestListQuery }
}

export type StripCounts = { total: number; overdue: number; byState: Record<RequestState, number> }

/** Contadores de `GET /requests/counts` con las etiquetas de la interfaz. */
export function toStripCounts(counts: RequestCounts | undefined): StripCounts {
  const byState = Object.fromEntries(REQUEST_STATES.map((s) => [s, 0])) as Record<RequestState, number>
  for (const status of STATUS_ORDER) byState[statusToState(status)] = counts?.byStatus[status] ?? 0
  return { total: counts?.total ?? 0, overdue: counts?.overdue ?? 0, byState }
}

/** Solicitudes abiertas según los contadores: las que aún no llegaron a "Resuelta". */
export const openCount = (counts: RequestCounts | undefined) =>
  counts ? OPEN_STATUSES.reduce((sum, s) => sum + (counts.byStatus[s] ?? 0), 0) : 0

/** CSV de las filas exportadas (se descarga desde el botón "Exportar"). */
export function toCsv(rows: RowView[]): string {
  return buildCsv(
    ['Código', 'Categoría', 'Ubicación', 'Prioridad', 'Estado', 'Responsable', 'Registro', 'Vencimiento', 'SLA', 'Descripción'],
    rows.map((r) => [
      r.code,
      r.category,
      r.location,
      r.priority,
      r.state,
      r.technician ?? 'Sin asignar',
      dateLong(r.createdAt),
      dateLong(r.dueAt),
      r.sla.label,
      r.description,
    ]),
  )
}
