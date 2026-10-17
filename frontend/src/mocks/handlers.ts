import { http, HttpResponse } from 'msw'
import type {
  CreateRequestBody,
  LoginBody,
  RequestDetail,
  RequestStatus,
  TransitionBody,
  UserSummary,
} from '../shared/api/types'
import { catalogs, demoPassword, roleIs, tokenFor, userFromToken, users } from './fixtures'

const api = '*/api/v1'

type StoredRequest = Omit<RequestDetail, 'allowedTransitions'> & { requesterId: number; areaId: number; technicianId: number | null }

let requests: StoredRequest[] = []
let sequence = 0

export function resetMockData() {
  requests = []
  sequence = 0
}

function problem(status: number, title: string, detail: string) {
  return HttpResponse.json({ type: 'about:blank', title, status, detail }, {
    status,
    headers: { 'Content-Type': 'application/problem+json' },
  })
}

function allowedTransitions(request: StoredRequest, user: UserSummary): RequestStatus[] {
  const supervisorOfArea = user.role === 'SUPERVISOR' && user.areaId === request.areaId
  const assignedTechnician = user.role === 'TECNICO' && user.id === request.technicianId
  const owner = user.role === 'SOLICITANTE' && user.id === request.requesterId
  switch (request.status) {
    case 'REGISTRADA':
      return supervisorOfArea ? ['EN_EVALUACION', 'CERRADA'] : []
    case 'EN_EVALUACION':
      return supervisorOfArea ? ['ASIGNADA', 'CERRADA'] : []
    case 'ASIGNADA':
      return supervisorOfArea ? ['ASIGNADA'] : assignedTechnician ? ['EN_ATENCION'] : []
    case 'EN_ATENCION':
      return assignedTechnician ? ['RESUELTA'] : []
    case 'RESUELTA':
      return owner ? ['CERRADA', 'EN_ATENCION'] : []
    default:
      return []
  }
}

function visible(request: StoredRequest, user: UserSummary): boolean {
  if (user.role === 'ADMINISTRADOR') return true
  if (user.role === 'SUPERVISOR') return request.areaId === user.areaId
  if (user.role === 'TECNICO') return request.technicianId === user.id
  return request.requesterId === user.id
}

function toDetail(request: StoredRequest, user: UserSummary): RequestDetail {
  return {
    ...toSummary(request),
    description: request.description,
    requester: request.requester,
    area: request.area,
    technician: request.technician,
    closureReason: request.closureReason,
    history: request.history,
    allowedTransitions: allowedTransitions(request, user),
  }
}

function toSummary(request: StoredRequest) {
  return {
    code: request.code,
    categoryName: request.categoryName,
    locationName: request.locationName,
    priorityName: request.priorityName,
    status: request.status,
    createdAt: request.createdAt,
    dueAt: request.dueAt,
    overdue: request.overdue,
  }
}

export const handlers = [
  http.post(`${api}/auth/login`, async ({ request }) => {
    const body = (await request.json()) as LoginBody
    const user = users.find((candidate) => candidate.email === body.email)
    if (!user || body.password !== demoPassword) return problem(401, 'Unauthorized', 'Credenciales inválidas')
    return HttpResponse.json({ accessToken: tokenFor(user), expiresIn: 900, user })
  }),

  http.post(`${api}/auth/refresh`, () => problem(401, 'Unauthorized', 'No hay sesión activa')),

  http.post(`${api}/auth/logout`, () => new HttpResponse(null, { status: 204 })),

  http.get(`${api}/me`, ({ request }) => {
    const user = userFromToken(request.headers.get('Authorization'))
    if (!user) return problem(401, 'Unauthorized', 'Authentication is required')
    return HttpResponse.json({ id: user.id, email: user.email, fullName: user.fullName, role: user.role })
  }),

  http.get(`${api}/catalogs`, ({ request }) => {
    if (!userFromToken(request.headers.get('Authorization'))) return problem(401, 'Unauthorized', 'Authentication is required')
    return HttpResponse.json(catalogs)
  }),

  http.get(`${api}/users/technicians`, ({ request }) => {
    const user = userFromToken(request.headers.get('Authorization'))
    if (!user) return problem(401, 'Unauthorized', 'Authentication is required')
    if (!roleIs(user, 'SUPERVISOR')) return problem(403, 'Forbidden', 'Solo supervisores')
    const areaId = Number(new URL(request.url).searchParams.get('areaId'))
    return HttpResponse.json(
      users.filter((u) => u.role === 'TECNICO' && u.areaId === areaId).map((u) => ({ id: u.id, fullName: u.fullName })),
    )
  }),

  http.get(`${api}/requests`, ({ request }) => {
    const user = userFromToken(request.headers.get('Authorization'))
    if (!user) return problem(401, 'Unauthorized', 'Authentication is required')
    const status = new URL(request.url).searchParams.get('status')
    const items = requests.filter((r) => visible(r, user) && (!status || r.status === status)).map(toSummary)
    return HttpResponse.json({ items, page: 0, size: 20, total: items.length })
  }),

  http.post(`${api}/requests`, async ({ request }) => {
    const user = userFromToken(request.headers.get('Authorization'))
    if (!user) return problem(401, 'Unauthorized', 'Authentication is required')
    if (!roleIs(user, 'SOLICITANTE')) return problem(403, 'Forbidden', 'Solo solicitantes')
    const body = (await request.json()) as CreateRequestBody
    if (body.description.trim().length < 20) return problem(400, 'Bad Request', 'La descripción debe tener al menos 20 caracteres')
    const category = catalogs.categories.find((c) => c.id === body.categoryId)!
    const priority = catalogs.priorities.find((p) => p.id === body.priorityId)!
    const area = catalogs.areas.find((a) => a.id === category.areaId)!
    const now = new Date()
    sequence += 1
    const stored: StoredRequest = {
      code: `SOL-${now.getUTCFullYear()}-${String(sequence).padStart(6, '0')}`,
      categoryName: category.name,
      locationName: catalogs.locations.find((l) => l.id === body.locationId)!.name,
      priorityName: priority.name,
      status: 'REGISTRADA',
      createdAt: now.toISOString(),
      dueAt: new Date(now.getTime() + priority.slaHours * 3_600_000).toISOString(),
      overdue: false,
      description: body.description,
      requester: { id: user.id, fullName: user.fullName },
      area,
      technician: null,
      closureReason: null,
      history: [{ fromStatus: null, toStatus: 'REGISTRADA', actorName: user.fullName, reason: null, createdAt: now.toISOString() }],
      requesterId: user.id,
      areaId: area.id,
      technicianId: null,
    }
    requests.push(stored)
    return HttpResponse.json(toDetail(stored, user), { status: 201 })
  }),

  http.get(`${api}/requests/:code`, ({ request, params }) => {
    const user = userFromToken(request.headers.get('Authorization'))
    if (!user) return problem(401, 'Unauthorized', 'Authentication is required')
    const found = requests.find((r) => r.code === params.code)
    if (!found || !visible(found, user)) return problem(404, 'Not Found', 'Solicitud no encontrada')
    return HttpResponse.json(toDetail(found, user))
  }),

  http.post(`${api}/requests/:code/transitions`, async ({ request, params }) => {
    const user = userFromToken(request.headers.get('Authorization'))
    if (!user) return problem(401, 'Unauthorized', 'Authentication is required')
    const found = requests.find((r) => r.code === params.code)
    if (!found || !visible(found, user)) return problem(404, 'Not Found', 'Solicitud no encontrada')
    const body = (await request.json()) as TransitionBody
    if (!allowedTransitions(found, user).includes(body.to)) return problem(409, 'Conflict', 'Transición no permitida')
    if (body.to === 'ASIGNADA') {
      const technician = users.find((u) => u.id === body.technicianId && u.role === 'TECNICO')
      if (!technician) return problem(400, 'Bad Request', 'Debe indicar un técnico del área')
      found.technicianId = technician.id
      found.technician = { id: technician.id, fullName: technician.fullName }
    }
    found.history.push({
      fromStatus: found.status,
      toStatus: body.to,
      actorName: user.fullName,
      reason: body.reason ?? null,
      createdAt: new Date().toISOString(),
    })
    found.status = body.to
    return HttpResponse.json(toDetail(found, user))
  }),
]
