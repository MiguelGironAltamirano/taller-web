import { beforeEach, describe, expect, it } from 'vitest'
import { ApiError, apiGet, apiPatch, apiPost } from '../shared/api/client'
import type {
  Catalogs,
  CreateRequestBody,
  Page,
  RequestDetail,
  RequestSummary,
  TokenResponse,
} from '../shared/api/types'
import { sessionStore } from '../shared/session/session-store'
import { server } from '../test/msw-server'
import { DEMO_PASSWORD } from './db'
import { handlers, resetMockData } from './handlers'

async function loginAs(email: string) {
  sessionStore.clear()
  const res = await apiPost<TokenResponse>('/auth/login', { email, password: DEMO_PASSWORD })
  sessionStore.set({ accessToken: res.accessToken, user: res.user })
  return res
}

const status = async (promise: Promise<unknown>) => {
  try {
    await promise
    return 200
  } catch (e) {
    return (e as ApiError).status
  }
}

const SOLICITANTE = 'lquispe@unmsm.edu.pe'
const SUPERVISOR = 'avillanueva@unmsm.edu.pe'
const TECNICO = 'cmendoza@unmsm.edu.pe'
const ADMIN = 'rsalazar@unmsm.edu.pe'

let catalogs: Catalogs
const validBody = (): CreateRequestBody => ({
  categoryId: catalogs.categories[0].id,
  locationId: catalogs.locations.find((l) => l.kind === 'AMBIENTE')!.id,
  description: 'El tomacorriente del aula no funciona desde ayer',
  priorityId: catalogs.priorities[1].id,
})

beforeEach(async () => {
  resetMockData()
  server.use(...handlers)
  await loginAs(SOLICITANTE)
  catalogs = await apiGet<Catalogs>('/catalogs')
})

describe('autenticación', () => {
  it('rechaza peticiones sin token con problem+json', async () => {
    // Sin sesión ni cookie de renovación (la simulada vive en sessionStorage).
    sessionStore.clear()
    sessionStorage.clear()
    expect(await status(apiGet('/requests'))).toBe(401)
  })

  it('rechaza credenciales inválidas con 401', async () => {
    expect(await status(apiPost('/auth/login', { email: SOLICITANTE, password: 'incorrecta' }))).toBe(401)
  })

  it('registra solo correos @unmsm.edu.pe y rechaza duplicados', async () => {
    const body = { fullName: 'Nuevo Usuario', email: 'nuevo@unmsm.edu.pe', password: 'Abcdef12', requesterType: 'DOCENTE' }
    expect(await status(apiPost('/auth/register', { ...body, email: 'nuevo@gmail.com' }))).toBe(400)
    expect(await status(apiPost('/auth/register', body))).toBe(200)
    expect(await status(apiPost('/auth/register', body))).toBe(409)
  })

  it('el autorregistro crea siempre un Solicitante', async () => {
    const body = { fullName: 'Nuevo Usuario', email: 'nuevo@unmsm.edu.pe', password: 'Abcdef12', requesterType: 'DOCENTE' }
    const created = await apiPost<{ role: string }>('/auth/register', body)
    expect(created.role).toBe('SOLICITANTE')
  })

  it('devuelve el usuario en /me', async () => {
    const me = await apiGet<{ email: string }>('/me')
    expect(me.email).toBe(SOLICITANTE)
  })
})

describe('catálogos', () => {
  it('devuelve áreas, categorías, prioridades con SLA y ubicaciones', () => {
    expect(catalogs.areas).toHaveLength(3)
    expect(catalogs.priorities.map((p) => [p.name, p.slaHours])).toEqual([
      ['Crítica', 8],
      ['Alta', 24],
      ['Media', 72],
      ['Baja', 120],
    ])
    expect(catalogs.locations.some((l) => l.kind === 'AMBIENTE')).toBe(true)
  })
})

describe('solicitudes', () => {
  it('crea una solicitud con código SOL-AAAA-NNNNNN y vencimiento = registro + SLA', async () => {
    const created = await apiPost<RequestDetail>('/requests', validBody())
    expect(created.code).toMatch(/^SOL-\d{4}-\d{6}$/)
    expect(created.status).toBe('REGISTRADA')
    const hours = (Date.parse(created.dueAt) - Date.parse(created.createdAt)) / 3_600_000
    expect(hours).toBe(24)
    expect(created.history).toHaveLength(1)
  })

  it('el área de la solicitud es la de la categoría elegida', async () => {
    const body = { ...validBody(), categoryId: catalogs.categories.find((c) => c.name === 'Red e internet')!.id }
    const created = await apiPost<RequestDetail>('/requests', body)
    expect(created.area.name).toBe('Soporte Informático')
  })

  it('valida la descripción: 19 → 400, 20 y 2000 → ok, 2001 → 400', async () => {
    const withDesc = (n: number) => apiPost('/requests', { ...validBody(), description: 'x'.repeat(n) })
    expect(await status(withDesc(19))).toBe(400)
    expect(await status(withDesc(20))).toBe(200)
    expect(await status(withDesc(2000))).toBe(200)
    expect(await status(withDesc(2001))).toBe(400)
  })

  it('genera códigos consecutivos y distintos', async () => {
    const codes: string[] = []
    for (let i = 0; i < 10; i++) codes.push((await apiPost<RequestDetail>('/requests', validBody())).code)
    expect(new Set(codes).size).toBe(10)
    const seqs = codes.map((c) => Number(c.slice(-6)))
    expect(seqs).toEqual([...seqs].sort((a, b) => a - b))
    expect(seqs[9] - seqs[0]).toBe(9)
  })

  it('solo el solicitante puede crear solicitudes', async () => {
    await loginAs(SUPERVISOR)
    expect(await status(apiPost('/requests', validBody()))).toBe(403)
  })

  it('un solicitante que pide una solicitud ajena recibe 404', async () => {
    await loginAs(ADMIN)
    const all = await apiGet<Page<RequestSummary>>('/requests', { size: 500 })
    const foreign = all.items.find((r) => r.requesterName !== 'Lucía Quispe Huamán')!
    await loginAs(SOLICITANTE)
    expect(await status(apiGet(`/requests/${foreign.code}`))).toBe(404)
  })

  it('el listado devuelve solo lo visible para cada rol', async () => {
    const list = async (email: string) => {
      await loginAs(email)
      return (await apiGet<Page<RequestSummary>>('/requests', { size: 500 })).items
    }
    const mine = await list(SOLICITANTE)
    expect(mine.length).toBeGreaterThan(0)
    expect(mine.every((r) => r.requesterName === 'Lucía Quispe Huamán')).toBe(true)

    const area = await list(SUPERVISOR)
    expect(area.every((r) => r.areaName === 'Mantenimiento e Infraestructura')).toBe(true)

    const assigned = await list(TECNICO)
    expect(assigned.every((r) => r.technicianName === 'Carlos Mendoza Rojas')).toBe(true)

    const everything = await list(ADMIN)
    expect(everything.length).toBeGreaterThan(area.length)
  })

  it('marca como vencida una solicitud sin resolver pasada su fecha, y no una resuelta', async () => {
    await loginAs(ADMIN)
    const all = (await apiGet<Page<RequestSummary>>('/requests', { size: 500 })).items
    expect(all.some((r) => r.overdue)).toBe(true)
    expect(all.filter((r) => r.status === 'RESUELTA' || r.status === 'CERRADA').every((r) => !r.overdue)).toBe(true)
  })
})

describe('flujo de estados', () => {
  async function registered() {
    const created = await apiPost<RequestDetail>('/requests', validBody())
    return created.code
  }
  const transition = (code: string, body: object) => apiPost<RequestDetail>(`/requests/${code}/transitions`, body)

  it('recorre el flujo completo con los roles correctos', async () => {
    const code = await registered()
    await loginAs(SUPERVISOR)
    expect((await transition(code, { to: 'EN_EVALUACION' })).status).toBe('EN_EVALUACION')

    const techs = await apiGet<{ id: number; fullName: string }[]>('/users/technicians', { areaId: 1 })
    const carlos = techs.find((t) => t.fullName === 'Carlos Mendoza Rojas')!
    const assigned = await transition(code, { to: 'ASIGNADA', technicianId: carlos.id })
    expect(assigned.technician?.fullName).toBe('Carlos Mendoza Rojas')

    await loginAs(TECNICO)
    expect((await transition(code, { to: 'EN_ATENCION' })).status).toBe('EN_ATENCION')
    const resolved = await transition(code, { to: 'RESUELTA', reason: 'Se cambió el tomacorriente' })
    expect(resolved.solution).toBe('Se cambió el tomacorriente')

    await loginAs(SOLICITANTE)
    const closed = await transition(code, { to: 'CERRADA' })
    expect(closed.status).toBe('CERRADA')
    expect(closed.closureReason).toBe('CONFIRMADA')
    expect(closed.history.length).toBe(6)
  })

  it('la calificación es opcional y solo se guarda una vez, después de confirmar la solución', async () => {
    const code = await registered()
    await loginAs(SUPERVISOR)
    await transition(code, { to: 'EN_EVALUACION' })
    const techs = await apiGet<{ id: number; fullName: string }[]>('/users/technicians', { areaId: 1 })
    await transition(code, { to: 'ASIGNADA', technicianId: techs[0].id })
    await loginAs(`${techs[0].fullName === 'Carlos Mendoza Rojas' ? 'cmendoza' : 'jpaucar'}@unmsm.edu.pe`)
    await transition(code, { to: 'EN_ATENCION' })
    await transition(code, { to: 'RESUELTA', reason: 'Se resolvió el problema' })
    await loginAs(SOLICITANTE)

    // Antes de confirmar no se puede calificar.
    expect(await status(apiPost(`/requests/${code}/rating`, { score: 4 }))).toBe(409)
    await transition(code, { to: 'CERRADA' })
    expect(await status(apiPost(`/requests/${code}/rating`, { score: 9 }))).toBe(400)

    const rated = await apiPost<RequestDetail>(`/requests/${code}/rating`, { score: 5, comment: 'Excelente' })
    expect(rated.rating?.score).toBe(5)
    expect(rated.history.length).toBe(6)
    expect(await status(apiPost(`/requests/${code}/rating`, { score: 3 }))).toBe(409)
  })

  it('rechaza con 409 una transición no listada', async () => {
    const code = await registered()
    await loginAs(SUPERVISOR)
    expect(await status(transition(code, { to: 'RESUELTA', reason: 'saltando pasos' }))).toBe(409)
  })

  it('rechaza con 403 a un rol que no puede ejecutar la transición', async () => {
    const code = await registered()
    expect(await status(transition(code, { to: 'EN_EVALUACION' }))).toBe(403)
  })

  it('exige técnico en asignar y motivo en reasignar y en cerrar', async () => {
    const code = await registered()
    await loginAs(SUPERVISOR)
    await transition(code, { to: 'EN_EVALUACION' })
    expect(await status(transition(code, { to: 'ASIGNADA' }))).toBe(400)
    expect(await status(transition(code, { to: 'CERRADA', closureReason: 'DUPLICADA' }))).toBe(400)
    const closed = await transition(code, { to: 'CERRADA', closureReason: 'DUPLICADA', reason: 'Igual a otra solicitud' })
    expect(closed.closureReason).toBe('DUPLICADA')
  })

  it('el cambio de prioridad recalcula el vencimiento', async () => {
    const code = await registered()
    await loginAs(SUPERVISOR)
    const critical = catalogs.priorities.find((p) => p.name === 'Crítica')!
    const changed = await apiPatch<RequestDetail>(`/requests/${code}/priority`, { priorityId: critical.id })
    expect((Date.parse(changed.dueAt) - Date.parse(changed.createdAt)) / 3_600_000).toBe(8)
    expect(changed.priorityName).toBe('Crítica')
  })

  it('el solicitante puede rechazar una solución y vuelve a En atención', async () => {
    await loginAs(ADMIN)
    const all = (await apiGet<Page<RequestSummary>>('/requests', { size: 500 })).items
    const resolved = all.find((r) => r.status === 'RESUELTA' && r.requesterName === 'Lucía Quispe Huamán')!
    await loginAs(SOLICITANTE)
    expect(await status(transition(resolved.code, { to: 'EN_ATENCION', reason: 'corto' }))).toBe(400)
    const back = await transition(resolved.code, { to: 'EN_ATENCION', reason: 'Sigue la fuga en el techo' })
    expect(back.status).toBe('EN_ATENCION')
  })

  it('genera notificaciones al cambiar de estado', async () => {
    const code = await registered()
    await loginAs(SUPERVISOR)
    await transition(code, { to: 'EN_EVALUACION' })
    await loginAs(SOLICITANTE)
    const notes = await apiGet<{ text: string }[]>('/notifications')
    expect(notes.some((n) => n.text.includes(code) && n.text.includes('En evaluación'))).toBe(true)
  })
})

describe('triage', () => {
  it('sugiere categoría y prioridad por palabras clave y nada sin coincidencias', async () => {
    const hit = await apiPost<{ categoryId: number; priorityId: number } | null>('/triage/suggestions', {
      description: 'Salen chispas y humo del cable del aula',
    })
    expect(hit).toEqual({
      categoryId: catalogs.categories.find((c) => c.name === 'Electricidad')!.id,
      priorityId: catalogs.priorities.find((p) => p.name === 'Crítica')!.id,
    })
    expect(await apiPost('/triage/suggestions', { description: 'Texto sin relación alguna con reglas' })).toBeNull()
  })
})

describe('administración', () => {
  it('solo el administrador accede a /admin', async () => {
    expect(await status(apiGet('/admin/users'))).toBe(403)
    await loginAs(ADMIN)
    expect(await status(apiGet('/admin/users'))).toBe(200)
  })

  it('exige área para técnicos y supervisores y correo institucional único', async () => {
    await loginAs(ADMIN)
    const base = { fullName: 'Nuevo Técnico', email: 'ntecnico@unmsm.edu.pe', role: 'TECNICO', areaId: null }
    expect(await status(apiPost('/admin/users', base))).toBe(400)
    expect(await status(apiPost('/admin/users', { ...base, areaId: 1 }))).toBe(200)
    expect(await status(apiPost('/admin/users', { ...base, areaId: 1 }))).toBe(400)
  })

  it('el dashboard calcula los indicadores del área del supervisor', async () => {
    await loginAs(SUPERVISOR)
    const res = await apiGet<{ areaName: string; kpis: { registered: { value: number } }; byPriority: unknown[] }>(
      '/dashboard/indicators',
      { range: '30' },
    )
    expect(res.areaName).toBe('Mantenimiento e Infraestructura')
    expect(res.kpis.registered.value).toBeGreaterThan(0)
    expect(res.byPriority).toHaveLength(4)
  })
})
