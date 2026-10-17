import { REQUEST_STATES, isFinalState, type Priority, type RequestState } from '../../../shared/domain'
import { buildCsv } from '../../../shared/lib/csv'
import { dateLong, normalizeText } from '../../../shared/lib/format'
import type { RowView } from './view'

export type ListView = 'all' | 'venc' | 'open' | 'confirm' | 'critsa'
export type ListSort = 'due' | 'new' | 'prio'

export type ListFilters = {
  q: string
  category: string
  priority: string
  technician: string
  /** Un estado, `__venc` (vencidas) o vacío. */
  status: string
  view: ListView
  sort: ListSort
}

export const VIEW_LABELS: Record<ListView, string> = {
  all: 'Todas',
  venc: 'Mis vencidas',
  open: 'Abiertas',
  confirm: 'Por confirmar',
  critsa: 'Críticas sin asignar',
}

export const OVERDUE_STATUS = '__venc'

const matchesView = (r: RowView, view: ListView): boolean => {
  switch (view) {
    case 'venc':
      return r.sla.overdue
    case 'open':
      return !isFinalState(r.state)
    case 'confirm':
      return r.state === 'Resuelta'
    case 'critsa':
      return r.priority === 'Crítica' && !r.technician && !isFinalState(r.state)
    default:
      return true
  }
}

/** Aplica búsqueda, filtros y vista guardada (la ordenación se hace aparte). */
export function filterRows(rows: RowView[], f: ListFilters): RowView[] {
  const q = normalizeText(f.q.trim())
  return rows.filter((r) => {
    if (q && !normalizeText(`${r.code} ${r.description} ${r.category} ${r.room}`).includes(q)) return false
    if (f.category && r.category !== f.category) return false
    if (f.priority && r.priority !== f.priority) return false
    if (f.technician && r.technician !== f.technician) return false
    if (f.status === OVERDUE_STATUS) {
      if (!r.sla.overdue) return false
    } else if (f.status && r.state !== f.status) return false
    return matchesView(r, f.view)
  })
}

const PRIORITY_ORDER: Record<Priority, number> = { Crítica: 0, Alta: 1, Media: 2, Baja: 3 }

/** Ordena sin mutar el original: por vencimiento (finalizadas al final), recientes o prioridad. */
export function sortRows(rows: RowView[], sort: ListSort): RowView[] {
  const copy = [...rows]
  if (sort === 'new') return copy.sort((a, b) => b.createdAt - a.createdAt)
  if (sort === 'prio') return copy.sort((a, b) => PRIORITY_ORDER[a.priority] - PRIORITY_ORDER[b.priority])
  return copy.sort((a, b) => Number(isFinalState(a.state)) - Number(isFinalState(b.state)) || a.dueAt - b.dueAt)
}

export function countByState(rows: RowView[]) {
  const byState = Object.fromEntries(REQUEST_STATES.map((s) => [s, 0])) as Record<RequestState, number>
  for (const r of rows) byState[r.state]++
  return { total: rows.length, overdue: rows.filter((r) => r.sla.overdue).length, byState }
}

/** CSV de las filas mostradas (se descarga desde el botón "Exportar"). */
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
