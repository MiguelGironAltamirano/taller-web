import { beforeEach, describe, expect, it } from 'vitest'
import { ApiError, apiGet, apiPost } from '../shared/api/client'
import { toQueryParams } from '../features/requests/api'
import type { Page, RequestCounts, RequestListQuery, RequestSummary, TokenResponse } from '../shared/api/types'
import { normalizeText } from '../shared/lib/format'
import { sessionStore } from '../shared/session/session-store'
import { server } from '../test/msw-server'
import { DEMO_PASSWORD, getDb } from './db'
import { handlers, resetMockData } from './handlers'

const SOLICITANTE = 'lquispe@unmsm.edu.pe'
const SUPERVISOR = 'avillanueva@unmsm.edu.pe'
const ADMIN = 'rsalazar@unmsm.edu.pe'

async function loginAs(email: string) {
  sessionStore.clear()
  const res = await apiPost<TokenResponse>('/auth/login', { email, password: DEMO_PASSWORD })
  sessionStore.set({ accessToken: res.accessToken, user: res.user })
}

const list = (query: RequestListQuery = {}) => apiGet<Page<RequestSummary>>('/requests', toQueryParams(query))
const everything = async () => (await list({ size: 500 })).items

async function problemOf(promise: Promise<unknown>) {
  try {
    await promise
  } catch (e) {
    return e as ApiError
  }
  throw new Error('Se esperaba un error')
}

beforeEach(async () => {
  resetMockData()
  server.use(...handlers)
  await loginAs(ADMIN)
})

describe('GET /requests (mock): filtros del servidor', () => {
  it('filtra por varios estados', async () => {
    const page = await list({ status: ['RESUELTA', 'CERRADA'], size: 500 })
    expect(page.items.length).toBeGreaterThan(0)
    expect(page.items.every((r) => r.status === 'RESUELTA' || r.status === 'CERRADA')).toBe(true)
    const all = await everything()
    expect(page.total).toBe(all.filter((r) => r.status === 'RESUELTA' || r.status === 'CERRADA').length)
  })

  it('filtra por categoría, prioridad, área y técnico usando ids', async () => {
    const db = getDb()
    const sample = db.requests.find((r) => r.technicianId !== null)!
    const category = db.categories.find((c) => c.id === sample.categoryId)!
    const byCategory = await list({ categoryId: category.id, size: 500 })
    expect(byCategory.items.every((r) => r.categoryName === category.name)).toBe(true)

    const priority = db.priorities.find((p) => p.id === sample.priorityId)!
    const byPriority = await list({ priorityId: priority.id, size: 500 })
    expect(byPriority.items.every((r) => r.priorityName === priority.name)).toBe(true)

    const area = db.areas.find((a) => a.id === category.areaId)!
    const byArea = await list({ areaId: area.id, size: 500 })
    expect(byArea.items.every((r) => r.areaName === area.name)).toBe(true)

    const technician = db.users.find((u) => u.id === sample.technicianId)!
    const byTechnician = await list({ technicianId: technician.id, size: 500 })
    expect(byTechnician.items.length).toBeGreaterThan(0)
    expect(byTechnician.items.every((r) => r.technicianName === technician.fullName)).toBe(true)
  })

  it('sin asignar y vencidas', async () => {
    const unassigned = await list({ unassigned: true, size: 500 })
    expect(unassigned.items.every((r) => r.technicianName === null)).toBe(true)
    const overdue = await list({ overdue: true, size: 500 })
    expect(overdue.items.length).toBeGreaterThan(0)
    expect(overdue.items.every((r) => r.overdue)).toBe(true)
    const onTime = await list({ overdue: false, size: 500 })
    expect(onTime.total + overdue.total).toBe((await everything()).length)
  })

  it('busca texto en código, descripción, solicitante, categoría y ambiente sin distinguir tildes', async () => {
    const [first] = await everything()
    expect((await list({ q: first.code })).items.map((r) => r.code)).toEqual([first.code])
    const byRequester = await list({ q: 'lucia quispe', size: 500 })
    expect(byRequester.items.length).toBeGreaterThan(0)
    expect(byRequester.items.every((r) => normalizeText(r.requesterName).includes('lucia quispe'))).toBe(true)
    expect((await list({ q: '%' })).total).toBe(0)
  })

  it('combina los filtros con AND', async () => {
    const all = await everything()
    const page = await list({ status: ['REGISTRADA', 'EN_EVALUACION'], unassigned: true, size: 500 })
    const expected = all.filter((r) => ['REGISTRADA', 'EN_EVALUACION'].includes(r.status) && r.technicianName === null)
    expect(page.items.map((r) => r.code).sort()).toEqual(expected.map((r) => r.code).sort())
  })

  it('ordena con una lista cerrada', async () => {
    const newest = (await list({ sort: 'newest', size: 500 })).items
    const created = newest.map((r) => Date.parse(r.createdAt))
    expect(created).toEqual([...created].sort((a, b) => b - a))
    const oldest = (await list({ sort: 'oldest', size: 500 })).items
    expect(oldest.map((r) => r.code)).toEqual([...newest].reverse().map((r) => r.code))
    const due = (await list({ sort: 'due', size: 500 })).items
    const firstFinished = due.findIndex((r) => r.status === 'RESUELTA' || r.status === 'CERRADA')
    expect(due.slice(firstFinished).every((r) => r.status === 'RESUELTA' || r.status === 'CERRADA')).toBe(true)
  })

  it('pagina después de filtrar e informa totalPages', async () => {
    const all = await everything()
    const page = await list({ page: 1, size: 3 })
    expect(page.total).toBe(all.length)
    expect(page.totalPages).toBe(Math.ceil(all.length / 3))
    expect(page.items.map((r) => r.code)).toEqual(
      (await list({ size: 500, sort: 'newest' })).items.slice(3, 6).map((r) => r.code),
    )
  })

  it('nunca amplía la visibilidad del rol', async () => {
    await loginAs(SUPERVISOR)
    const own = await everything()
    const foreignArea = getDb().areas.find((a) => !own.some((r) => r.areaName === a.name))!
    expect((await list({ areaId: foreignArea.id })).total).toBe(0)
    await loginAs(SOLICITANTE)
    const technician = getDb().users.find((u) => u.role === 'TECNICO')!
    const mine = await list({ technicianId: technician.id, size: 500 })
    expect(mine.items.every((r) => r.requesterName === 'Lucía Quispe Huamán')).toBe(true)
  })

  it('rechaza parámetros inválidos con errores por campo', async () => {
    const error = await problemOf(
      apiGet('/requests', { status: 'ABIERTA', categoryId: 'abc', overdue: 'si', sort: 'random', page: -1, size: 501 }),
    )
    expect(error.status).toBe(400)
    expect(Object.keys(error.fieldErrors ?? {}).sort()).toEqual(['categoryId', 'overdue', 'page', 'size', 'sort', 'status'])
    expect((await problemOf(apiGet('/requests', { technicianId: 3, unassigned: true }))).status).toBe(400)
    expect((await problemOf(apiGet('/requests', { from: '2026-10-02', to: '2026-10-01' }))).status).toBe(400)
  })
})

describe('GET /requests/counts (mock)', () => {
  it('cuenta por estado y vencidas dentro de la visibilidad del rol', async () => {
    const counts = await apiGet<RequestCounts>('/requests/counts')
    const all = await everything()
    expect(counts.total).toBe(all.length)
    expect(counts.overdue).toBe(all.filter((r) => r.overdue).length)
    expect(counts.byStatus.RESUELTA).toBe(all.filter((r) => r.status === 'RESUELTA').length)
    expect(Object.keys(counts.byStatus)).toHaveLength(7)

    await loginAs(SOLICITANTE)
    const mine = await apiGet<RequestCounts>('/requests/counts')
    expect(mine.total).toBe((await everything()).length)
    expect(mine.total).toBeLessThan(counts.total)
  })
})
