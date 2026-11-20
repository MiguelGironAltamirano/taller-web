import { beforeEach, describe, expect, it } from 'vitest'
import { ApiError, apiDelete, apiGet, apiPatch, apiPost, apiPut } from '../shared/api/client'
import type {
  AdminUser,
  AppConfig,
  AuditEvent,
  Catalogs,
  CreateRequestBody,
  DashboardIndicators,
  Page,
  RequestDetail,
  RequestSummary,
  TeamMemberLoad,
  TokenResponse,
  TriageRuleDto,
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
    expect(created.estimatedDue).toBe(true)
    expect((Date.parse(created.dueAt) - Date.parse(created.createdAt)) / 3_600_000).toBe(24)
    expect(created.history).toHaveLength(1)

    await loginAs(SUPERVISOR)
    const staff = await apiGet<RequestDetail>(`/requests/${created.code}`)
    expect(staff.estimatedDue).toBe(false)
    expect((Date.parse(staff.dueAt) - Date.parse(staff.createdAt)) / 3_600_000).toBe(24)
  })

  it('el solicitante no se entera del cambio de prioridad del supervisor', async () => {
    const { code } = await apiPost<RequestDetail>('/requests', validBody())
    await loginAs(SUPERVISOR)
    const critical = catalogs.priorities.find((p) => p.name === 'Crítica')!
    await apiPatch(`/requests/${code}/priority`, { priorityId: critical.id })

    await loginAs(SOLICITANTE)
    const detail = await apiGet<RequestDetail>(`/requests/${code}`)
    expect(detail.priorityName).toBe('Alta')
    expect((Date.parse(detail.dueAt) - Date.parse(detail.createdAt)) / 3_600_000).toBe(24)
    expect(detail.overdue).toBe(false)
    expect(detail.history.some((h) => h.note)).toBe(false)
    const alta = catalogs.priorities.find((p) => p.name === 'Alta')!
    const list = await apiGet<Page<RequestSummary>>(`/requests?priorityId=${alta.id}`)
    expect(list.items.map((r) => r.code)).toContain(code)
    expect(list.items.find((r) => r.code === code)?.priorityName).toBe('Alta')
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

  it('el listado filtra por rango de fechas (días de Lima, inclusive) y por área', async () => {
    await loginAs(ADMIN)
    const all = (await apiGet<Page<RequestSummary>>('/requests', { size: 500 })).items
    const day = (iso: string) => new Date(new Date(iso).getTime() - 5 * 3_600_000).toISOString().slice(0, 10)
    const target = day(all[0].createdAt)

    const sameDay = (await apiGet<Page<RequestSummary>>('/requests', { size: 500, from: target, to: target })).items
    expect(sameDay.length).toBeGreaterThan(0)
    expect(sameDay.every((r) => day(r.createdAt) === target)).toBe(true)

    expect((await apiGet<Page<RequestSummary>>('/requests', { size: 500, from: '2999-01-01' })).items).toEqual([])
    expect(await status(apiGet('/requests', { from: '2026-10-15', to: '2026-10-01' }))).toBe(400)

    const maintenance = (await apiGet<Page<RequestSummary>>('/requests', { size: 500, areaId: 1 })).items
    expect(maintenance.length).toBeGreaterThan(0)
    expect(maintenance.length).toBeLessThan(all.length)
    expect(maintenance.every((r) => r.areaName === 'Mantenimiento e Infraestructura')).toBe(true)
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
    expect(assigned.autoCloseAt).toBeNull()
    expect(Date.parse(resolved.autoCloseAt!) - Date.parse(resolved.history.at(-1)!.createdAt)).toBe(72 * 3_600_000)

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
    expect(changed.history.at(-1)?.note).toBe('Prioridad Alta → Prioridad Crítica')
    // Elegir la misma prioridad no es un cambio (igual que el backend).
    expect(await status(apiPatch(`/requests/${code}/priority`, { priorityId: critical.id }))).toBe(400)
  })

  it('el solicitante puede rechazar una solución y vuelve a En atención', async () => {
    await loginAs(ADMIN)
    const all = (await apiGet<Page<RequestSummary>>('/requests', { size: 500 })).items
    const resolved = all.find((r) => r.status === 'RESUELTA' && r.requesterName === 'Lucía Quispe Huamán')!
    await loginAs(SOLICITANTE)
    expect(await status(transition(resolved.code, { to: 'EN_ATENCION', reason: 'corto' }))).toBe(400)
    const back = await transition(resolved.code, { to: 'EN_ATENCION', reason: 'Sigue la fuga en el techo' })
    expect(back.status).toBe('EN_ATENCION')
    expect(back.solution).toBeNull()
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

  it('valida la descripción y solo responde al solicitante', async () => {
    expect(await status(apiPost('/triage/suggestions', { description: '   ' }))).toBe(400)
    expect(await status(apiPost('/triage/suggestions', { description: 'x'.repeat(2001) }))).toBe(400)
    await loginAs(ADMIN)
    expect(await status(apiPost('/triage/suggestions', { description: 'No hay luz en el aula' }))).toBe(403)
  })

  it('las reglas se guardan limpias y validan categoría activa, prioridad y palabras clave', async () => {
    await loginAs(ADMIN)
    const categoryId = catalogs.categories[0].id
    const priorityId = catalogs.priorities[0].id
    const created = await apiPost<TriageRuleDto>('/admin/triage-rules', {
      keywords: ['  Zumbido ', 'ZUMBIDO', 'Ruido   Fuerte'],
      categoryId,
      priorityId,
    })
    expect(created).toMatchObject({ keywords: ['zumbido', 'ruido fuerte'], active: true })
    const ids = (await apiGet<TriageRuleDto[]>('/admin/triage-rules')).map((r) => r.id)
    expect(ids).toEqual([...ids].sort((a, b) => a - b))

    const invalid = await apiPost('/admin/triage-rules', { keywords: ['x'], categoryId: 999, priorityId: 999 }).catch(
      (e: ApiError) => e,
    )
    expect(invalid).toBeInstanceOf(ApiError)
    expect(Object.keys((invalid as ApiError).fieldErrors ?? {})).toEqual(['keywords', 'categoryId', 'priorityId'])

    expect(await status(apiDelete(`/admin/triage-rules/${created.id}`))).toBe(200)
    expect(await status(apiDelete(`/admin/triage-rules/${created.id}`))).toBe(404)
  })
})

describe('administración', () => {
  it('solo el administrador accede a /admin', async () => {
    expect(await status(apiGet('/admin/users'))).toBe(403)
    await loginAs(ADMIN)
    expect(await status(apiGet('/admin/users'))).toBe(200)
  })

  it('exige área para técnicos y supervisores y correo institucional único (409 si se repite)', async () => {
    await loginAs(ADMIN)
    const base = { fullName: 'Nuevo Técnico', email: 'ntecnico@unmsm.edu.pe', role: 'TECNICO', areaId: null }
    expect(await status(apiPost('/admin/users', base))).toBe(400)
    expect(await status(apiPost('/admin/users', { ...base, areaId: 1 }))).toBe(200)
    expect(await status(apiPost('/admin/users', { ...base, areaId: 1 }))).toBe(409)
  })

  it('rechaza un área para solicitantes y administradores', async () => {
    await loginAs(ADMIN)
    const body = { fullName: 'Ana Pérez', email: 'aperez@unmsm.edu.pe', role: 'SOLICITANTE', areaId: 1 }
    expect(await status(apiPost('/admin/users', body))).toBe(400)
  })

  it('devuelve una contraseña temporal solo al crear y sirve para iniciar sesión', async () => {
    await loginAs(ADMIN)
    const created = await apiPost<AdminUser>('/admin/users', {
      fullName: 'Nuevo Técnico',
      email: 'ntecnico@unmsm.edu.pe',
      role: 'TECNICO',
      areaId: 1,
    })
    expect(created.temporaryPassword).toMatch(/^[A-Za-z0-9]{12}$/)
    const listed = await apiGet<AdminUser[]>('/admin/users')
    expect(listed.every((u) => u.temporaryPassword === undefined)).toBe(true)

    sessionStore.clear()
    const session = await apiPost<TokenResponse>('/auth/login', {
      email: 'ntecnico@unmsm.edu.pe',
      password: created.temporaryPassword,
    })
    expect(session.user.role).toBe('TECNICO')
  })

  it('áreas: 409 por nombre repetido o si tienen categorías activas; sin uso se eliminan', async () => {
    await loginAs(ADMIN)
    const catalogs = await apiGet<Catalogs>('/catalogs')
    const used = catalogs.areas.find((a) => catalogs.categories.some((c) => c.areaId === a.id))!
    expect(await status(apiPost('/admin/areas', { name: used.name.toUpperCase() }))).toBe(409)
    expect(await status(apiDelete(`/admin/areas/${used.id}`))).toBe(409)
    const created = await apiPost<{ id: number }>('/admin/areas', { name: 'Biblioteca' })
    expect(await status(apiDelete(`/admin/areas/${created.id}`))).toBe(200)
    expect(await status(apiDelete(`/admin/areas/${created.id}`))).toBe(404)
  })

  it('categorías: 409 por nombre repetido y se pueden desactivar aunque tengan solicitudes', async () => {
    await loginAs(ADMIN)
    const catalogs = await apiGet<Catalogs>('/catalogs')
    const category = catalogs.categories[0]
    expect(await status(apiPost('/admin/categories', { name: category.name, areaId: category.areaId }))).toBe(409)
    expect(await status(apiPost('/admin/categories', { name: 'Nueva', areaId: 9999 }))).toBe(400)
    expect(await status(apiDelete(`/admin/categories/${category.id}`))).toBe(200)
  })

  it('prioridades: horas de SLA entre 1 y 8760', async () => {
    await loginAs(ADMIN)
    const [priority] = (await apiGet<Catalogs>('/catalogs')).priorities
    expect(await status(apiPut(`/admin/priorities/${priority.id}`, { slaHours: 8761 }))).toBe(400)
    expect(await status(apiPut(`/admin/priorities/${priority.id}`, { slaHours: 48 }))).toBe(200)
  })

  it('ubicaciones: 409 por ambiente repetido y solo se eliminan ambientes', async () => {
    await loginAs(ADMIN)
    const body = { faculty: 'Facultad de Letras', pavilion: 'Pabellón B', room: 'Aula 305' }
    const room = await apiPost<{ id: number; parentId: number }>('/admin/locations', body)
    expect(await status(apiPost('/admin/locations', { ...body, room: 'aula 305' }))).toBe(409)
    expect(await status(apiDelete(`/admin/locations/${room.parentId}`))).toBe(409)
    expect(await status(apiDelete(`/admin/locations/${room.id}`))).toBe(200)
  })

  it('configuración: valores por defecto como en el backend y horas mayores a 0', async () => {
    await loginAs(ADMIN)
    expect(await apiGet<AppConfig>('/admin/config')).toEqual({ autoCloseHours: 72, emailNotifications: false, triageSuggestions: true })
    expect(await status(apiPut('/admin/config', { autoCloseHours: 0, emailNotifications: false, triageSuggestions: true }))).toBe(400)
    const saved = await apiPut<AppConfig>('/admin/config', { autoCloseHours: 48, emailNotifications: true, triageSuggestions: false })
    expect(saved.autoCloseHours).toBe(48)
  })

  it('auditoría: máximo 40 eventos, más recientes primero y sin separador vacío', async () => {
    await loginAs(ADMIN)
    const events = await apiGet<AuditEvent[]>('/admin/audit', { q: '' })
    expect(events.length).toBeGreaterThan(0)
    expect(events.length).toBeLessThanOrEqual(40)
    expect([...events].sort((a, b) => b.createdAt.localeCompare(a.createdAt))).toEqual(events)
    expect(events.every((e) => !e.detail.endsWith('· '))).toBe(true)
  })

  it('el administrador no puede desactivarse ni cambiar su propio rol', async () => {
    const { user } = await loginAs(ADMIN)
    expect(await status(apiPatch(`/admin/users/${user.id}/active`, { active: false }))).toBe(409)
    expect(
      await status(apiPut(`/admin/users/${user.id}`, { fullName: user.fullName, email: ADMIN, role: 'SOLICITANTE', areaId: null })),
    ).toBe(409)
    expect(await status(apiGet('/admin/users'))).toBe(200)
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

  it('el dashboard sigue el contrato: una serie por día, timeline coherente y dueSoon acotado', async () => {
    await loginAs(ADMIN)
    for (const range of ['7', '30', 'mes'] as const) {
      const res = await apiGet<DashboardIndicators>('/dashboard/indicators', { range })
      expect(res.areaName).toBeNull()
      expect(res.timeline).toHaveLength(res.rangeDays)
      for (const kpi of Object.values(res.kpis)) expect(kpi.series).toHaveLength(res.rangeDays)
      expect(res.timeline.reduce((a, b) => a + b, 0)).toBe(res.kpis.registered.value)
      expect(res.kpis.pending.value + res.kpis.attended.value).toBeLessThanOrEqual(res.kpis.registered.value)
      expect(res.dueSoon.length).toBeLessThanOrEqual(10)
    }
    expect((await apiGet<DashboardIndicators>('/dashboard/indicators', { range: '7' })).rangeDays).toBe(7)
  })

  it('el dashboard rechaza un rango inválido o un área inexistente, y otros roles', async () => {
    await loginAs(ADMIN)
    expect(await status(apiGet('/dashboard/indicators', { range: '15' }))).toBe(400)
    expect(await status(apiGet('/dashboard/indicators', { areaId: 999 }))).toBe(400)
    expect(await status(apiGet('/dashboard/team'))).toBe(403)
    await loginAs(TECNICO)
    expect(await status(apiGet('/dashboard/indicators'))).toBe(403)
  })

  it('el equipo separa asignadas y en atención, con capacidad 8', async () => {
    await loginAs(SUPERVISOR)
    const team = await apiGet<TeamMemberLoad[]>('/dashboard/team')
    expect(team.length).toBeGreaterThan(0)
    for (const t of team) {
      expect(t.capacity).toBe(8)
      expect(t.overdue).toBeLessThanOrEqual(t.assigned + t.inProgress)
    }
  })
})
