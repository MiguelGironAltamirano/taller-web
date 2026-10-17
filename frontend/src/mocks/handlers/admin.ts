import { http, HttpResponse } from 'msw'
import type {
  AdminUser,
  AdminUserBody,
  AppConfig,
  AuditEvent,
  StateInfo,
  TriageRuleBody,
  TriageRuleDto,
} from '../../shared/api/types'
import { STATUS_ORDER } from '../../shared/api/mappers'
import { codeOf, getDb, type MockTriageRule, type MockUser } from '../db'
import { iso, notFound, problem, secured } from '../http'

const ADMIN: ['ADMINISTRADOR'] = ['ADMINISTRADOR']
const INSTITUTIONAL = /^[^\s@]+@unmsm\.edu\.pe$/i
const STATE_DESCRIPTIONS = [
  'Solicitud creada',
  'El supervisor evalúa el caso',
  'Técnico asignado',
  'El técnico trabaja en la solicitud',
  'Solución propuesta, pendiente de confirmación',
  'Solicitud finalizada',
]

const toAdminUser = (u: MockUser): AdminUser => ({
  id: u.id,
  fullName: u.fullName,
  email: u.email,
  role: u.role,
  areaId: u.areaId,
  areaName: getDb().areas.find((a) => a.id === u.areaId)?.name ?? null,
  requesterType: u.requesterType,
  active: u.active,
})

const toRuleDto = (r: MockTriageRule): TriageRuleDto => {
  const db = getDb()
  return {
    id: r.id,
    keywords: r.keywords,
    categoryId: r.categoryId,
    categoryName: db.categories.find((c) => c.id === r.categoryId)?.name ?? '',
    priorityId: r.priorityId,
    priorityName: db.priorities.find((p) => p.id === r.priorityId)?.name ?? '',
    active: r.active,
  }
}

const belongsToArea = (role: AdminUserBody['role']) => role === 'TECNICO' || role === 'SUPERVISOR'

/** Mismas reglas que el backend (contrato: "Administración: usuarios y estados"). */
function validateUser(body: AdminUserBody): Record<string, string> {
  const errors: Record<string, string> = {}
  const fullName = body.fullName?.trim() ?? ''
  if (fullName.length < 3 || fullName.length > 150) errors.fullName = 'El nombre completo debe tener entre 3 y 150 caracteres'
  if (!INSTITUTIONAL.test(body.email?.trim() ?? '')) errors.email = 'El correo debe terminar en @unmsm.edu.pe'
  if (belongsToArea(body.role) && !body.areaId) errors.areaId = 'El área es obligatoria para este rol'
  else if (!belongsToArea(body.role) && body.areaId) errors.areaId = 'Este rol no pertenece a un área'
  else if (body.areaId && !activeArea(body.areaId)) errors.areaId = 'El área no existe o está inactiva'
  return errors
}

const activeArea = (id: number) => getDb().areas.find((a) => a.id === id && a.active !== false)
const sameName = (a: string, b: string) => a.trim().toLowerCase() === b.trim().toLowerCase()

const emailTaken = (email: string, ignoreId?: number) =>
  getDb().users.some((u) => u.email === email.trim().toLowerCase() && u.id !== ignoreId)

const emailConflict = () =>
  problem(409, 'Conflict', 'El correo ya está registrado', { email: 'El correo ya está registrado' })

const PASSWORD_LETTERS = 'ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz'
const PASSWORD_ALPHABET = PASSWORD_LETTERS + '23456789'

/** 12 caracteres con al menos una letra y un dígito, como la contraseña temporal del backend. */
function temporaryPassword(): string {
  const random = crypto.getRandomValues(new Uint32Array(12))
  const chars = Array.from(random, (n) => PASSWORD_ALPHABET[n % PASSWORD_ALPHABET.length])
  chars[random[0] % 6] = PASSWORD_LETTERS[random[1] % PASSWORD_LETTERS.length]
  chars[6 + (random[2] % 6)] = String(2 + (random[3] % 8))
  return chars.join('')
}

export const adminHandlers = [
  // ───── Usuarios ─────
  http.get('*/api/v1/admin/users', secured(ADMIN, () => HttpResponse.json(getDb().users.map(toAdminUser)))),

  http.post(
    '*/api/v1/admin/users',
    secured(ADMIN, async ({ request }) => {
      const db = getDb()
      const body = (await request.json()) as AdminUserBody
      const errors = validateUser(body)
      if (Object.keys(errors).length) return problem(400, 'Datos no válidos', Object.values(errors)[0], errors)
      if (emailTaken(body.email)) return emailConflict()
      const password = temporaryPassword()
      const user: MockUser = {
        id: ++db.nextId,
        fullName: body.fullName.trim(),
        email: body.email.trim().toLowerCase(),
        role: body.role,
        areaId: body.areaId,
        requesterType: body.role === 'SOLICITANTE' ? (body.requesterType ?? 'ESTUDIANTE') : null,
        active: true,
        password,
      }
      db.users.push(user)
      return HttpResponse.json({ ...toAdminUser(user), temporaryPassword: password }, { status: 201 })
    }),
  ),

  http.put(
    '*/api/v1/admin/users/:id',
    secured(ADMIN, async ({ request, params, user: actor }) => {
      const user = getDb().users.find((u) => u.id === Number(params.id))
      if (!user) return notFound('El usuario no existe')
      const body = (await request.json()) as AdminUserBody
      const errors = validateUser(body)
      if (Object.keys(errors).length) return problem(400, 'Datos no válidos', Object.values(errors)[0], errors)
      if (emailTaken(body.email, user.id)) return emailConflict()
      if (actor.id === user.id && body.role !== user.role) return problem(409, 'Conflict', 'No puedes cambiar tu propio rol')
      Object.assign(user, {
        fullName: body.fullName.trim(),
        email: body.email.trim().toLowerCase(),
        role: body.role,
        areaId: body.areaId,
        requesterType: body.role === 'SOLICITANTE' ? (body.requesterType ?? user.requesterType ?? 'ESTUDIANTE') : null,
      })
      return HttpResponse.json(toAdminUser(user))
    }),
  ),

  http.patch(
    '*/api/v1/admin/users/:id/active',
    secured(ADMIN, async ({ request, params, user: actor }) => {
      const db = getDb()
      const user = db.users.find((u) => u.id === Number(params.id))
      if (!user) return notFound('El usuario no existe')
      const { active } = (await request.json()) as { active: boolean }
      if (actor.id === user.id && !active) return problem(409, 'Conflict', 'No puedes desactivar tu propia cuenta')
      user.active = active
      if (!active) {
        const admins = db.users.filter((u) => u.role === 'ADMINISTRADOR').map((u) => u.id)
        for (const recipientId of admins) {
          db.notifications.unshift({
            id: ++db.nextId,
            recipientId,
            text: `Se desactivó la cuenta de ${user.fullName}.`,
            createdAt: Date.now(),
            read: false,
            requestCode: null,
          })
        }
      }
      return HttpResponse.json(toAdminUser(user))
    }),
  ),

  // ───── Catálogos ─────
  http.post(
    '*/api/v1/admin/areas',
    secured(ADMIN, async ({ request }) => {
      const db = getDb()
      const { name } = (await request.json()) as { name: string }
      const clean = name?.trim() ?? ''
      if (!clean) return problem(400, 'Datos no válidos', 'Ingresa el nombre.', { name: 'Ingresa el nombre.' })
      const existing = db.areas.find((a) => sameName(a.name, clean))
      if (existing?.active === false) {
        existing.active = true
        return HttpResponse.json({ id: existing.id, name: existing.name }, { status: 201 })
      }
      if (existing) {
        return problem(409, 'Conflict', 'Ya existe un área con ese nombre', { name: 'Ya existe un área con ese nombre' })
      }
      const area = { id: ++db.nextId, name: clean }
      db.areas.push(area)
      return HttpResponse.json(area, { status: 201 })
    }),
  ),

  http.delete(
    '*/api/v1/admin/areas/:id',
    secured(ADMIN, ({ params }) => {
      const db = getDb()
      const id = Number(params.id)
      const area = activeArea(id)
      if (!area) return notFound('El área no existe')
      if (db.categories.some((c) => c.areaId === id && c.active !== false)) {
        return problem(409, 'Conflict', 'No se puede eliminar: el área tiene categorías activas')
      }
      if (db.users.some((u) => u.areaId === id && u.active && belongsToArea(u.role))) {
        return problem(409, 'Conflict', 'No se puede eliminar: el área tiene técnicos o supervisores activos')
      }
      area.active = false
      return new HttpResponse(null, { status: 204 })
    }),
  ),

  http.post(
    '*/api/v1/admin/categories',
    secured(ADMIN, async ({ request }) => {
      const db = getDb()
      const body = (await request.json()) as { name: string; areaId: number }
      const errors: Record<string, string> = {}
      const name = body.name?.trim() ?? ''
      if (!name) errors.name = 'Ingresa el nombre.'
      if (!activeArea(body.areaId)) errors.areaId = 'El área no existe o está inactiva'
      if (Object.keys(errors).length) return problem(400, 'Datos no válidos', Object.values(errors)[0], errors)
      const existing = db.categories.find((c) => sameName(c.name, name))
      if (existing?.active === false) {
        Object.assign(existing, { active: true, areaId: body.areaId })
        return HttpResponse.json({ id: existing.id, name: existing.name, areaId: existing.areaId }, { status: 201 })
      }
      if (existing) {
        return problem(409, 'Conflict', 'Ya existe una categoría con ese nombre', { name: 'Ya existe una categoría con ese nombre' })
      }
      const category = { id: ++db.nextId, name, areaId: body.areaId }
      db.categories.push(category)
      return HttpResponse.json(category, { status: 201 })
    }),
  ),

  http.delete(
    '*/api/v1/admin/categories/:id',
    secured(ADMIN, ({ params }) => {
      const db = getDb()
      const id = Number(params.id)
      const category = db.categories.find((c) => c.id === id && c.active !== false)
      if (!category) return notFound('La categoría no existe')
      // Se desactiva aunque tenga solicitudes: estas la conservan y deja de ofrecerse al registrar.
      category.active = false
      return new HttpResponse(null, { status: 204 })
    }),
  ),

  http.put(
    '*/api/v1/admin/priorities/:id',
    secured(ADMIN, async ({ request, params }) => {
      const priority = getDb().priorities.find((p) => p.id === Number(params.id))
      if (!priority) return notFound()
      const { slaHours } = (await request.json()) as { slaHours: number }
      if (!Number.isInteger(slaHours) || slaHours <= 0 || slaHours > 8760) {
        return problem(400, 'Datos no válidos', 'Las horas de SLA deben estar entre 1 y 8760', {
          slaHours: 'Las horas de SLA deben estar entre 1 y 8760',
        })
      }
      priority.slaHours = slaHours
      return HttpResponse.json(priority)
    }),
  ),

  http.post(
    '*/api/v1/admin/locations',
    secured(ADMIN, async ({ request }) => {
      const db = getDb()
      const body = (await request.json()) as { faculty: string; pavilion: string; room: string }
      const errors: Record<string, string> = {}
      if (!body.faculty?.trim()) errors.faculty = 'Ingresa la facultad.'
      if (!body.pavilion?.trim()) errors.pavilion = 'Ingresa el pabellón.'
      if (!body.room?.trim()) errors.room = 'Ingresa el ambiente.'
      if (Object.keys(errors).length) return problem(400, 'Datos no válidos', Object.values(errors)[0], errors)

      const ensure = (name: string, kind: 'FACULTAD' | 'PABELLON' | 'AMBIENTE', parentId: number | null) => {
        const existing = db.locations.find((l) => l.kind === kind && l.parentId === parentId && l.name.toLowerCase() === name.toLowerCase())
        if (existing) return existing
        const created = { id: ++db.nextId, name, kind, parentId }
        db.locations.push(created)
        return created
      }
      const faculty = ensure(body.faculty.trim(), 'FACULTAD', null)
      const pavilion = ensure(body.pavilion.trim(), 'PABELLON', faculty.id)
      const roomName = body.room.trim()
      const sameRoom = (l: { kind: string; parentId: number | null; name: string }) =>
        l.kind === 'AMBIENTE' && l.parentId === pavilion.id && l.name.toLowerCase() === roomName.toLowerCase()
      if (db.locations.some(sameRoom)) {
        return problem(409, 'Conflict', 'El ambiente ya existe en ese pabellón', { room: 'El ambiente ya existe en ese pabellón' })
      }
      const room = ensure(roomName, 'AMBIENTE', pavilion.id)
      return HttpResponse.json(room, { status: 201 })
    }),
  ),

  http.delete(
    '*/api/v1/admin/locations/:id',
    secured(ADMIN, ({ params }) => {
      const db = getDb()
      const id = Number(params.id)
      const room = db.locations.find((l) => l.id === id)
      if (!room) return notFound('La ubicación no existe')
      if (room.kind !== 'AMBIENTE') return problem(409, 'Conflict', 'Solo se pueden eliminar ambientes')
      if (db.requests.some((r) => r.locationId === id)) {
        return problem(409, 'Conflict', 'No se puede eliminar: hay solicitudes en este ambiente')
      }
      db.locations = db.locations.filter((l) => l.id !== id)
      // Limpia pabellones y facultades que quedaron vacíos.
      if (room.parentId) {
        const prune = (parentId: number | null) => {
          if (parentId === null) return
          if (!db.locations.some((l) => l.parentId === parentId)) {
            const node = db.locations.find((l) => l.id === parentId)
            db.locations = db.locations.filter((l) => l.id !== parentId)
            prune(node?.parentId ?? null)
          }
        }
        prune(room.parentId)
      }
      return new HttpResponse(null, { status: 204 })
    }),
  ),

  http.get(
    '*/api/v1/admin/states',
    secured(ADMIN, () => {
      const items: StateInfo[] = STATUS_ORDER.map((status, i) => ({ order: i + 1, status, description: STATE_DESCRIPTIONS[i] }))
      return HttpResponse.json(items)
    }),
  ),

  // ───── Reglas de triage ─────
  http.get('*/api/v1/admin/triage-rules', secured(ADMIN, () => HttpResponse.json(getDb().rules.map(toRuleDto)))),

  http.post(
    '*/api/v1/admin/triage-rules',
    secured(ADMIN, async ({ request }) => {
      const db = getDb()
      const body = (await request.json()) as TriageRuleBody
      const errors = validateRule(body)
      if (Object.keys(errors).length) return problem(400, 'Datos no válidos', Object.values(errors)[0], errors)
      const rule: MockTriageRule = {
        id: ++db.nextId,
        keywords: cleanKeywords(body.keywords),
        categoryId: body.categoryId,
        priorityId: body.priorityId,
        active: body.active ?? true,
      }
      db.rules.push(rule)
      return HttpResponse.json(toRuleDto(rule), { status: 201 })
    }),
  ),

  http.put(
    '*/api/v1/admin/triage-rules/:id',
    secured(ADMIN, async ({ request, params }) => {
      const rule = getDb().rules.find((r) => r.id === Number(params.id))
      if (!rule) return notFound()
      const body = (await request.json()) as TriageRuleBody
      const errors = validateRule(body)
      if (Object.keys(errors).length) return problem(400, 'Datos no válidos', Object.values(errors)[0], errors)
      Object.assign(rule, {
        keywords: cleanKeywords(body.keywords),
        categoryId: body.categoryId,
        priorityId: body.priorityId,
        active: body.active ?? rule.active,
      })
      return HttpResponse.json(toRuleDto(rule))
    }),
  ),

  http.delete(
    '*/api/v1/admin/triage-rules/:id',
    secured(ADMIN, ({ params }) => {
      const db = getDb()
      db.rules = db.rules.filter((r) => r.id !== Number(params.id))
      return new HttpResponse(null, { status: 204 })
    }),
  ),

  // ───── Auditoría y configuración ─────
  http.get(
    '*/api/v1/admin/audit',
    secured(ADMIN, ({ request }) => {
      const q = (new URL(request.url).searchParams.get('q') ?? '').trim().toLowerCase()
      const events: AuditEvent[] = []
      for (const r of getDb().requests) {
        for (const h of r.history) {
          const from = h.note ? h.note.split('→')[0].trim() : (h.fromStatus ?? '—')
          const to = h.note ? (h.note.split('→')[1] ?? '').trim() : h.toStatus
          events.push({
            id: events.length + 1,
            createdAt: iso(h.createdAt),
            actorName: h.actorName,
            requestCode: codeOf(r),
            detail: `${from} → ${to} · ${h.reason ?? ''}`,
          })
        }
      }
      const items = events
        .sort((a, b) => b.createdAt.localeCompare(a.createdAt))
        .filter((e) => !q || `${e.actorName}${e.requestCode}${e.detail}`.toLowerCase().includes(q))
        .slice(0, 40)
      return HttpResponse.json(items)
    }),
  ),

  http.get('*/api/v1/admin/config', secured(ADMIN, () => HttpResponse.json(getDb().config))),

  http.put(
    '*/api/v1/admin/config',
    secured(ADMIN, async ({ request }) => {
      const body = (await request.json()) as AppConfig
      if (!Number.isInteger(body.autoCloseHours) || body.autoCloseHours <= 0) {
        return problem(400, 'Datos no válidos', 'Ingresa horas mayores a 0.', { autoCloseHours: 'Ingresa horas mayores a 0.' })
      }
      getDb().config = { ...body }
      return HttpResponse.json(getDb().config)
    }),
  ),
]

const cleanKeywords = (keywords: string[]) => keywords.map((k) => k.trim()).filter(Boolean)

function validateRule(body: TriageRuleBody): Record<string, string> {
  const db = getDb()
  const errors: Record<string, string> = {}
  if (!cleanKeywords(body.keywords ?? []).length) errors.keywords = 'Ingresa al menos una palabra clave.'
  if (!db.categories.some((c) => c.id === body.categoryId)) errors.categoryId = 'Selecciona la categoría.'
  if (!db.priorities.some((p) => p.id === body.priorityId)) errors.priorityId = 'Selecciona la prioridad.'
  return errors
}
