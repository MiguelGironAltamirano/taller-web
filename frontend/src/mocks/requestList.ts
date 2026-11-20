import { STATUS_ORDER } from '../shared/api/mappers'
import {
  MAX_REQUEST_PAGE_SIZE,
  type Page,
  type RequestCounts,
  type RequestListQuery,
  type RequestSortParam,
  type RequestStatus,
  type RequestSummary,
} from '../shared/api/types'
import { normalizeText } from '../shared/lib/format'
import { getDb, type MockRequest, type MockUser } from './db'
import { areaIdOf, inScope, requestedDueAtOf, requestedPriorityIdOf, summaryOf } from './views'

/**
 * Misma semántica que `GET /requests` del backend (`RequestListCriteria` + `JdbcRequestQueries`): filtros con AND
 * dentro de la visibilidad del rol, orden de una lista cerrada, paginación validada y errores por campo.
 */

const SORTS: RequestSortParam[] = ['newest', 'oldest', 'due', 'priority']
const FINISHED: RequestStatus[] = ['RESUELTA', 'CERRADA']
const DEFAULT_SIZE = 20
const MAX_TEXT = 100

export type ParsedListQuery = Required<Pick<RequestListQuery, 'sort' | 'page' | 'size'>> & RequestListQuery

export function parseListQuery(params: URLSearchParams): { query: ParsedListQuery } | { errors: Record<string, string> } {
  const errors: Record<string, string> = {}
  const text = (name: string) => params.getAll(name).join(',').trim()

  const status: RequestStatus[] = []
  for (const part of params.getAll('status').flatMap((v) => v.split(','))) {
    const name = part.trim()
    if (!name) continue
    if ((STATUS_ORDER as string[]).includes(name)) status.push(name as RequestStatus)
    else errors.status = `Estado desconocido: ${name}. Valores válidos: ${STATUS_ORDER.join(', ')}`
  }
  const id = (name: string) => {
    const raw = text(name)
    if (!raw) return undefined
    if (/^[1-9]\d*$/.test(raw)) return Number(raw)
    errors[name] = 'Debe ser un identificador numérico positivo'
    return undefined
  }
  const flag = (name: string) => {
    const raw = text(name)
    if (!raw) return undefined
    if (raw === 'true' || raw === 'false') return raw === 'true'
    errors[name] = 'Debe ser true o false'
    return undefined
  }
  const day = (name: string) => {
    const raw = text(name)
    if (!raw) return undefined
    if (/^\d{4}-\d{2}-\d{2}$/.test(raw) && !Number.isNaN(Date.parse(raw)) && new Date(raw).toISOString().startsWith(raw)) {
      return raw
    }
    errors[name] = 'Debe ser una fecha AAAA-MM-DD, p. ej. 2026-10-01'
    return undefined
  }
  const integer = (name: string, fallback: number, min: number, max: number) => {
    const raw = text(name)
    if (!raw) return fallback
    const n = Number(raw)
    if (/^-?\d+$/.test(raw) && n >= min && n <= max) return n
    errors[name] = max === Number.MAX_SAFE_INTEGER ? `Debe ser un entero mayor o igual que ${min}` : `Debe ser un entero entre ${min} y ${max}`
    return fallback
  }

  const technicianId = id('technicianId')
  const unassigned = flag('unassigned')
  const q = text('q')
  if (q.length > MAX_TEXT) errors.q = `La búsqueda admite hasta ${MAX_TEXT} caracteres`
  const from = day('from')
  const to = day('to')
  if (from && to && to < from) errors.to = 'La fecha final no puede ser anterior a la inicial'
  const rawSort = text('sort')
  if (rawSort && !(SORTS as string[]).includes(rawSort)) errors.sort = `Orden desconocido. Valores válidos: ${SORTS.join(', ')}`
  if (unassigned && technicianId !== undefined) errors.unassigned = 'No se puede combinar con technicianId'

  const query: ParsedListQuery = {
    status: status.length ? status : undefined,
    areaId: id('areaId'),
    categoryId: id('categoryId'),
    priorityId: id('priorityId'),
    technicianId,
    unassigned,
    overdue: flag('overdue'),
    q: q || undefined,
    from,
    to,
    sort: (rawSort || 'newest') as RequestSortParam,
    page: integer('page', 0, 0, Number.MAX_SAFE_INTEGER),
    size: integer('size', DEFAULT_SIZE, 1, MAX_REQUEST_PAGE_SIZE),
  }
  return Object.keys(errors).length ? { errors } : { query }
}

const DAY_MS = 24 * 3_600_000

/** Inicio del día `AAAA-MM-DD` en hora de Lima (UTC-5, sin horario de verano), como lo calcula el servidor. */
const limaDayStart = (day: string) => Date.parse(`${day}T00:00:00-05:00`)

const isOverdue = (r: MockRequest, now: number) => !FINISHED.includes(r.status) && r.dueAt < now

/** El solicitante filtra y ordena por la prioridad que eligió y su fecha estimada; el personal, por las vigentes. */
const priorityFor = (r: MockRequest, requester: boolean) => (requester ? requestedPriorityIdOf(r) : r.priorityId)
const dueFor = (r: MockRequest, requester: boolean) => (requester ? requestedDueAtOf(r) : r.dueAt)

function matches(r: MockRequest, s: RequestSummary, f: ParsedListQuery, now: number, requester: boolean): boolean {
  if (f.status && !f.status.includes(r.status)) return false
  if (f.areaId !== undefined && areaIdOf(r) !== f.areaId) return false
  if (f.categoryId !== undefined && r.categoryId !== f.categoryId) return false
  if (f.priorityId !== undefined && priorityFor(r, requester) !== f.priorityId) return false
  if (f.technicianId !== undefined && r.technicianId !== f.technicianId) return false
  if (f.unassigned && r.technicianId !== null) return false
  if (f.overdue !== undefined && isOverdue(r, now) !== f.overdue) return false
  if (f.from && r.createdAt < limaDayStart(f.from)) return false
  if (f.to && r.createdAt >= limaDayStart(f.to) + DAY_MS) return false
  if (f.q) {
    const haystack = [s.code, s.description, s.requesterName, s.categoryName, s.roomName].map(normalizeText)
    if (!haystack.some((field) => field.includes(normalizeText(f.q!)))) return false
  }
  return true
}

/** Igual que el backend: cada orden termina en un desempate estable (el número de solicitud). */
function compare(sort: RequestSortParam, requester: boolean) {
  const newest = (a: MockRequest, b: MockRequest) => b.createdAt - a.createdAt || b.seq - a.seq
  switch (sort) {
    case 'oldest':
      return (a: MockRequest, b: MockRequest) => -newest(a, b)
    case 'due':
      return (a: MockRequest, b: MockRequest) =>
        Number(FINISHED.includes(a.status)) - Number(FINISHED.includes(b.status)) ||
        dueFor(a, requester) - dueFor(b, requester) ||
        a.seq - b.seq
    case 'priority':
      // Los ids de prioridad del mock siguen el orden de importancia (1 = Crítica).
      return (a: MockRequest, b: MockRequest) => priorityFor(a, requester) - priorityFor(b, requester) || newest(a, b)
    default:
      return newest
  }
}

export function listRequests(user: MockUser, query: ParsedListQuery, now = Date.now()): Page<RequestSummary> {
  const requester = user.role === 'SOLICITANTE'
  // Como el backend: para el solicitante nada está vencido, así que `overdue` se ignora.
  const f: ParsedListQuery = requester ? { ...query, overdue: undefined } : query
  const all = getDb()
    .requests.filter((r) => inScope(r, user))
    .map((r) => ({ r, s: summaryOf(r, now, user) }))
    .filter(({ r, s }) => matches(r, s, f, now, requester))
    .sort((a, b) => compare(f.sort, requester)(a.r, b.r))
  const start = f.page * f.size
  return {
    items: all.slice(start, start + f.size).map(({ s }) => s),
    page: f.page,
    size: f.size,
    total: all.length,
    totalPages: Math.ceil(all.length / f.size),
  }
}

export function countRequests(user: MockUser, now = Date.now()): RequestCounts {
  const byStatus = Object.fromEntries(STATUS_ORDER.map((s) => [s, 0])) as Record<RequestStatus, number>
  let overdue = 0
  let total = 0
  for (const r of getDb().requests.filter((x) => inScope(x, user))) {
    byStatus[r.status]++
    total++
    if (user.role !== 'SOLICITANTE' && isOverdue(r, now)) overdue++
  }
  return { total, overdue, byStatus }
}
