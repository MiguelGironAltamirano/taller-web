import { http, HttpResponse } from 'msw'
import type {
  DashboardIndicators,
  KpiValue,
  NotificationDto,
  TeamMemberLoad,
  TriageSuggestionDto,
} from '../../shared/api/types'
import { matchTriage } from '../../features/triage/lib/suggest'
import { PRIORITIES } from '../../shared/domain'
import { getDb, HOUR, type MockRequest } from '../db'
import { iso, notFound, problem, secured } from '../http'
import { areaIdOf, summaryOf } from '../views'

const DAY = 24 * HOUR

export const notificationHandlers = [
  http.get(
    '*/api/v1/notifications',
    secured(null, ({ user }) => {
      const items: NotificationDto[] = getDb()
        .notifications.filter((n) => n.recipientId === user.id)
        .sort((a, b) => b.createdAt - a.createdAt)
        .map((n) => ({ id: n.id, text: n.text, createdAt: iso(n.createdAt), read: n.read, requestCode: n.requestCode }))
      return HttpResponse.json(items)
    }),
  ),

  http.patch(
    '*/api/v1/notifications/:id/read',
    secured(null, ({ params, user }) => {
      const n = getDb().notifications.find((x) => x.id === Number(params.id) && x.recipientId === user.id)
      if (!n) return notFound()
      n.read = true
      return new HttpResponse(null, { status: 204 })
    }),
  ),

  http.post(
    '*/api/v1/notifications/read-all',
    secured(null, ({ user }) => {
      for (const n of getDb().notifications) if (n.recipientId === user.id) n.read = true
      return new HttpResponse(null, { status: 204 })
    }),
  ),
]

export const triageHandlers = [
  http.post(
    '*/api/v1/triage/suggestions',
    secured(['SOLICITANTE'], async ({ request }) => {
      const db = getDb()
      const { description } = (await request.json()) as { description?: string }
      if (typeof description !== 'string' || !description.trim() || description.length > 2000) {
        const message = 'La descripción debe tener entre 1 y 2000 caracteres.'
        return problem(400, 'Datos no válidos', message, { description: message })
      }
      if (!db.config.triageSuggestions) return HttpResponse.json<TriageSuggestionDto>(null)
      // Igual que el backend: solo reglas activas cuya categoría siga activa.
      const rules = db.rules.filter((r) => db.categories.some((c) => c.id === r.categoryId && c.active !== false))
      const hit = matchTriage(description, rules)
      return HttpResponse.json<TriageSuggestionDto>(hit ? { categoryId: hit.categoryId, priorityId: hit.priorityId } : null)
    }),
  ),
]

// ───── Dashboard: same rules as the backend (docs/api/contrato-v1.md, "Dashboard (indicadores)") ─────

/** America/Lima is UTC-5 all year (no daylight saving time). */
const LIMA_OFFSET = 5 * HOUR
const TECHNICIAN_CAPACITY = 8
const DUE_SOON_LIMIT = 10

/** Day number of an instant counted in Lima, so periods start at Lima midnight. */
const limaDay = (ms: number) => Math.floor((ms - LIMA_OFFSET) / DAY)
const round1 = (n: number) => Math.round(n * 10) / 10
const percentage = (part: number, total: number) => (total ? round1((part * 100) / total) : 0)
const isOpen = (r: MockRequest) => r.status !== 'RESUELTA' && r.status !== 'CERRADA'

/** First time it reached RESUELTA, only while it is still resolved or closed. */
const attendedAt = (r: MockRequest) => (isOpen(r) ? null : (r.history.find((h) => h.toStatus === 'RESUELTA')?.createdAt ?? null))

function rangeDays(range: string, now: number): number | null {
  if (range === '7') return 7
  if (range === '30') return 30
  if (range === 'mes') return new Date(now - LIMA_OFFSET).getUTCDate()
  return null
}

export const dashboardHandlers = [
  http.get(
    '*/api/v1/dashboard/indicators',
    secured(['SUPERVISOR', 'ADMINISTRADOR'], ({ request, user }) => {
      const db = getDb()
      const params = new URL(request.url).searchParams
      const now = Date.now()
      const days = rangeDays(params.get('range') || '30', now)
      if (days === null) return problem(400, 'Bad Request', 'La consulta contiene parámetros inválidos', { range: 'El rango debe ser 7, 30 o mes' })
      const areaId = user.role === 'SUPERVISOR' ? user.areaId : Number(params.get('areaId')) || null
      const area = areaId ? db.areas.find((a) => a.id === areaId) : null
      if (areaId && !area && user.role === 'ADMINISTRADOR') {
        return problem(400, 'Bad Request', 'La consulta contiene parámetros inválidos', { areaId: 'El área no existe o está inactiva' })
      }

      const scope = db.requests.filter((r) => !areaId || areaIdOf(r) === areaId)
      const firstDay = limaDay(now) - days + 1
      const dayIndex = (r: MockRequest) => limaDay(r.createdAt) - firstDay
      const current = scope.filter((r) => dayIndex(r) >= 0 && dayIndex(r) < days)
      const previous = scope.filter((r) => dayIndex(r) >= -days && dayIndex(r) < 0)
      const byDay = Array.from({ length: days }, (_, i) => current.filter((r) => dayIndex(r) === i))

      const isOverdue = (r: MockRequest) => summaryOf(r, now).overdue
      const attended = (list: MockRequest[]) => list.filter((r) => attendedAt(r) !== null)
      const metrics: Record<keyof DashboardIndicators['kpis'], (list: MockRequest[]) => number> = {
        registered: (list) => list.length,
        pending: (list) => list.filter(isOpen).length,
        attended: (list) => attended(list).length,
        resolvedPct: (list) => percentage(attended(list).length, list.length),
        avgAttentionHours: (list) => {
          const done = attended(list)
          return done.length ? round1(done.reduce((acc, r) => acc + (attendedAt(r)! - r.createdAt) / HOUR, 0) / done.length) : 0
        },
        overdue: (list) => list.filter(isOverdue).length,
      }
      const kpi = (metric: (list: MockRequest[]) => number): KpiValue => {
        const value = metric(current)
        const before = metric(previous)
        return { value, deltaPct: before ? round1(((value - before) / before) * 100) : 0, series: byDay.map(metric) }
      }

      const summaries = current.map((r) => summaryOf(r, now))
      const count = (pick: (s: (typeof summaries)[number]) => string) => {
        const m = new Map<string, number>()
        for (const s of summaries) m.set(pick(s), (m.get(pick(s)) ?? 0) + 1)
        return [...m.entries()]
          .sort((a, b) => b[1] - a[1] || a[0].localeCompare(b[0]))
          .map(([label, value]) => ({ label, value }))
      }
      const done = attended(current)

      const body: DashboardIndicators = {
        rangeDays: days,
        areaName: area?.name ?? null,
        kpis: {
          registered: kpi(metrics.registered),
          pending: kpi(metrics.pending),
          attended: kpi(metrics.attended),
          resolvedPct: kpi(metrics.resolvedPct),
          avgAttentionHours: kpi(metrics.avgAttentionHours),
          overdue: kpi(metrics.overdue),
        },
        byCategory: count((s) => s.categoryName),
        byPriority: PRIORITIES.map((label) => ({ label, value: summaries.filter((s) => s.priorityName === label).length })),
        byTechnician: count((s) => s.technicianName ?? 'Sin asignar'),
        timeline: byDay.map((list) => list.length),
        slaCompliancePct: percentage(done.filter((r) => attendedAt(r)! < r.dueAt).length, done.length),
        dueSoon: scope
          .filter((r) => isOpen(r) && r.dueAt > now && r.dueAt <= now + DAY)
          .sort((a, b) => a.dueAt - b.dueAt)
          .slice(0, DUE_SOON_LIMIT)
          .map((r) => summaryOf(r, now)),
      }
      return HttpResponse.json(body)
    }),
  ),

  http.get(
    '*/api/v1/dashboard/team',
    secured(['SUPERVISOR'], ({ user }) => {
      const db = getDb()
      const now = Date.now()
      const items: TeamMemberLoad[] = db.users
        .filter((u) => u.role === 'TECNICO' && u.active && u.areaId === user.areaId)
        .sort((a, b) => a.fullName.localeCompare(b.fullName))
        .map((u) => {
          const mine = db.requests.filter((r) => r.technicianId === u.id && isOpen(r) && areaIdOf(r) === user.areaId)
          return {
            id: u.id,
            fullName: u.fullName,
            assigned: mine.filter((r) => r.status === 'ASIGNADA').length,
            inProgress: mine.filter((r) => r.status === 'EN_ATENCION').length,
            overdue: mine.filter((r) => r.dueAt < now).length,
            capacity: TECHNICIAN_CAPACITY,
          }
        })
      return HttpResponse.json(items)
    }),
  ),
]
