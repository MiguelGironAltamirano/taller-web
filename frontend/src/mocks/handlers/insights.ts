import { http, HttpResponse } from 'msw'
import type {
  DashboardIndicators,
  KpiValue,
  NotificationDto,
  TeamMemberLoad,
  TriageSuggestionDto,
} from '../../shared/api/types'
import { suggestTriage } from '../../features/triage/lib/suggest'
import { PRIORITIES, type Priority } from '../../shared/domain'
import { getDb, HOUR } from '../db'
import { iso, notFound, secured } from '../http'
import { areaIdOf, inScope, summaryOf } from '../views'

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
    secured(null, async ({ request }) => {
      const db = getDb()
      const { description } = (await request.json()) as { description: string }
      if (!db.config.triageSuggestions) return HttpResponse.json<TriageSuggestionDto>(null)
      const rules = db.rules.map((r) => ({
        id: r.id,
        keywords: r.keywords,
        category: db.categories.find((c) => c.id === r.categoryId)?.name ?? '',
        priority: (db.priorities.find((p) => p.id === r.priorityId)?.name ?? 'Media') as Priority,
        active: r.active,
      }))
      const hit = suggestTriage(description ?? '', rules)
      if (!hit) return HttpResponse.json<TriageSuggestionDto>(null)
      return HttpResponse.json<TriageSuggestionDto>({
        categoryId: db.categories.find((c) => c.name === hit.category)!.id,
        priorityId: db.priorities.find((p) => p.name === hit.priority)!.id,
      })
    }),
  ),
]

const DELTAS = [12, -8, 15, 4, -6, -20]

export const dashboardHandlers = [
  http.get(
    '*/api/v1/dashboard/indicators',
    secured(['SUPERVISOR', 'ADMINISTRADOR'], ({ request, user }) => {
      const db = getDb()
      const params = new URL(request.url).searchParams
      const range = params.get('range') ?? '30'
      const now = Date.now()
      const dayOfMonth = new Date(now).getDate()
      const days = range === '7' ? 7 : range === 'mes' ? Math.max(1, dayOfMonth) : 30
      const areaId = user.role === 'SUPERVISOR' ? user.areaId : Number(params.get('areaId')) || null

      const base = db.requests.filter((r) => inScope(r, user) && (!areaId || areaIdOf(r) === areaId))
      const inRange = base.filter((r) => r.createdAt >= now - days * DAY)
      const summaries = inRange.map((r) => summaryOf(r, now))
      const done = inRange.filter((r) => r.status === 'RESUELTA' || r.status === 'CERRADA')
      const total = inRange.length
      const overdue = summaries.filter((s) => s.overdue).length
      const avgHours = done.length
        ? Math.round(done.reduce((acc, r) => acc + (r.history[r.history.length - 1].createdAt - r.createdAt) / HOUR, 0) / done.length)
        : 0

      const buckets = days === 7 ? 7 : 15
      const bucketDays = days / buckets
      const series = Array<number>(buckets).fill(0)
      for (const r of inRange) {
        const i = Math.min(buckets - 1, buckets - 1 - Math.floor((now - r.createdAt) / DAY / bucketDays))
        if (i >= 0) series[i]++
      }
      const spark = (k: number) => series.map((v, j) => Math.max(0, v + ((j * (k + 2)) % 3) - 1))
      const kpi = (value: number, k: number): KpiValue => ({ value, deltaPct: DELTAS[k], series: spark(k) })

      const count = <T extends string>(pick: (s: (typeof summaries)[number]) => T) => {
        const m = new Map<T, number>()
        for (const s of summaries) m.set(pick(s), (m.get(pick(s)) ?? 0) + 1)
        return [...m.entries()].sort((a, b) => b[1] - a[1]).map(([label, value]) => ({ label, value }))
      }

      const body: DashboardIndicators = {
        rangeDays: days,
        areaName: areaId ? (db.areas.find((a) => a.id === areaId)?.name ?? null) : null,
        kpis: {
          registered: kpi(total, 0),
          pending: kpi(inRange.filter((r) => r.status !== 'RESUELTA' && r.status !== 'CERRADA').length, 1),
          attended: kpi(done.length, 2),
          resolvedPct: kpi(total ? Math.round((done.length / total) * 100) : 0, 3),
          avgAttentionHours: kpi(avgHours, 4),
          overdue: kpi(overdue, 5),
        },
        byCategory: count((s) => s.categoryName).slice(0, 6),
        byPriority: PRIORITIES.map((label) => ({ label, value: summaries.filter((s) => s.priorityName === label).length })),
        byTechnician: count((s) => s.technicianName ?? 'Sin asignar').slice(0, 6),
        timeline: series,
        slaCompliancePct: total ? Math.round(((total - overdue) / total) * 100) : 100,
        dueSoon: base
          .filter((r) => r.status !== 'RESUELTA' && r.status !== 'CERRADA' && r.dueAt > now && r.dueAt < now + DAY)
          .sort((a, b) => a.dueAt - b.dueAt)
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
        .filter((u) => u.role === 'TECNICO' && u.areaId === user.areaId)
        .map((u) => {
          const mine = db.requests.filter((r) => r.technicianId === u.id && r.status !== 'RESUELTA' && r.status !== 'CERRADA')
          return {
            id: u.id,
            fullName: u.fullName,
            assigned: mine.length,
            inProgress: mine.filter((r) => r.status === 'EN_ATENCION').length,
            overdue: mine.filter((r) => r.dueAt < now).length,
            capacity: 6,
          }
        })
      return HttpResponse.json(items)
    }),
  ),
]
