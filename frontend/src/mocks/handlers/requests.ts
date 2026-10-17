import { http, HttpResponse } from 'msw'
import type {
  ChangePriorityBody,
  CreateCommentBody,
  CreateRequestBody,
  Page,
  RateBody,
  RequestStatus,
  TransitionBody,
} from '../../shared/api/types'
import { statusToState } from '../../shared/api/mappers'
import { computeDueAt } from '../../features/workflow/lib/sla'
import { codeOf, getDb, type MockRequest, type MockUser } from '../db'
import { forbidden, notFound, problem, secured } from '../http'
import { areaIdOf, detailOf, findByCode, inScope, notify, summaryOf } from '../views'

const actorLabel = (u: MockUser) =>
  u.role === 'SUPERVISOR' ? `${u.fullName} (Supervisor)` : u.role === 'ADMINISTRADOR' ? `${u.fullName} (Administrador)` : u.fullName

type Kind = 'eval' | 'assign' | 'reassign' | 'start' | 'resolve' | 'confirm' | 'reject' | 'close'

/** Transiciones listadas en spec §6; cualquier otra devuelve 409. */
function kindOf(from: RequestStatus, to: RequestStatus): Kind | null {
  if (from === 'REGISTRADA' && to === 'EN_EVALUACION') return 'eval'
  if (from === 'EN_EVALUACION' && to === 'ASIGNADA') return 'assign'
  if (from === 'ASIGNADA' && to === 'ASIGNADA') return 'reassign'
  if (from === 'ASIGNADA' && to === 'EN_ATENCION') return 'start'
  if (from === 'EN_ATENCION' && to === 'RESUELTA') return 'resolve'
  if (from === 'RESUELTA' && to === 'CERRADA') return 'confirm'
  if (from === 'RESUELTA' && to === 'EN_ATENCION') return 'reject'
  if ((from === 'REGISTRADA' || from === 'EN_EVALUACION') && to === 'CERRADA') return 'close'
  return null
}

const supervisorsOf = (areaId: number) =>
  getDb()
    .users.filter((u) => u.role === 'SUPERVISOR' && u.areaId === areaId && u.active)
    .map((u) => u.id)

const pushHistory = (
  r: MockRequest,
  entry: { from: RequestStatus | null; to: RequestStatus; actor: string; reason: string | null; note?: string },
) =>
  r.history.push({
    fromStatus: entry.from,
    toStatus: entry.to,
    actorName: entry.actor,
    reason: entry.reason,
    createdAt: Date.now(),
    note: entry.note,
  })

export const requestHandlers = [
  http.get(
    '*/api/v1/requests',
    secured(null, ({ request, user }) => {
      const url = new URL(request.url)
      const status = url.searchParams.get('status')
      const page = Math.max(0, Number(url.searchParams.get('page') ?? 0))
      const size = Math.min(500, Math.max(1, Number(url.searchParams.get('size') ?? 20)))
      const all = getDb()
        .requests.filter((r) => inScope(r, user) && (!status || r.status === status))
        .sort((a, b) => a.dueAt - b.dueAt)
        .map((r) => summaryOf(r))
      const body: Page<ReturnType<typeof summaryOf>> = { items: all.slice(page * size, page * size + size), page, size, total: all.length }
      return HttpResponse.json(body)
    }),
  ),

  http.post(
    '*/api/v1/requests',
    secured(['SOLICITANTE'], async ({ request, user }) => {
      const db = getDb()
      const body = (await request.json()) as CreateRequestBody
      const errors: Record<string, string> = {}
      const description = body.description?.trim() ?? ''
      const category = db.categories.find((c) => c.id === body.categoryId)
      const location = db.locations.find((l) => l.id === body.locationId)
      const priority = db.priorities.find((p) => p.id === body.priorityId)
      if (!category) errors.categoryId = 'Selecciona una categoría.'
      if (!location || location.kind !== 'AMBIENTE') errors.locationId = 'Selecciona el ambiente.'
      if (!priority) errors.priorityId = 'Selecciona una prioridad.'
      if (description.length < 20 || description.length > 2000) {
        errors.description = 'La descripción debe tener entre 20 y 2000 caracteres.'
      }
      if ((body.evidences?.length ?? 0) > 5) errors.evidences = 'Máximo 5 archivos.'
      if (Object.keys(errors).length) return problem(400, 'Datos no válidos', 'Revisa los campos marcados.', errors)

      const now = Date.now()
      const year = new Date(now).getFullYear()
      const seq = Math.max(0, ...db.requests.filter((r) => r.year === year).map((r) => r.seq)) + 1
      const created: MockRequest = {
        seq,
        year,
        requesterId: user.id,
        categoryId: category!.id,
        locationId: location!.id,
        priorityId: priority!.id,
        status: 'REGISTRADA',
        createdAt: now,
        dueAt: computeDueAt(now, priority!.slaHours),
        technicianId: null,
        description,
        closureReason: null,
        solution: null,
        rating: null,
        comments: [],
        evidences: (body.evidences ?? []).map((e) => ({
          id: ++db.nextId,
          fileName: e.fileName,
          kind: /\.pdf$/i.test(e.fileName) ? ('PDF' as const) : ('IMG' as const),
          sizeBytes: e.sizeBytes,
        })),
        history: [],
      }
      pushHistory(created, { from: null, to: 'REGISTRADA', actor: user.fullName, reason: 'Solicitud creada' })
      db.requests.unshift(created)
      notify(supervisorsOf(category!.areaId), `Nueva solicitud registrada: ${codeOf(created)} (${category!.name}).`, codeOf(created))
      return HttpResponse.json(detailOf(created, user), { status: 201 })
    }),
  ),

  http.get(
    '*/api/v1/requests/:code',
    secured(null, ({ params, user }) => {
      const r = findByCode(String(params.code))
      return r && inScope(r, user) ? HttpResponse.json(detailOf(r, user)) : notFound()
    }),
  ),

  http.post(
    '*/api/v1/requests/:code/transitions',
    secured(null, async ({ request, params, user }) => {
      const db = getDb()
      const r = findByCode(String(params.code))
      if (!r || !inScope(r, user)) return notFound()
      const body = (await request.json()) as TransitionBody
      const kind = kindOf(r.status, body.to)
      if (!kind) {
        return problem(409, 'Transición inválida', `No se puede pasar de ${statusToState(r.status)} a ${body.to ? statusToState(body.to) : '—'}.`)
      }

      const isSupervisor = user.role === 'SUPERVISOR' && areaIdOf(r) === user.areaId
      const isTechnician = user.role === 'TECNICO' && r.technicianId === user.id
      const isOwner = user.role === 'SOLICITANTE' && r.requesterId === user.id
      const allowed =
        kind === 'eval' || kind === 'assign' || kind === 'reassign' || kind === 'close'
          ? isSupervisor
          : kind === 'start' || kind === 'resolve'
            ? isTechnician
            : isOwner
      if (!allowed) return forbidden()

      const reason = body.reason?.trim() ?? ''
      const errors: Record<string, string> = {}
      const technician = body.technicianId ? db.users.find((u) => u.id === body.technicianId) : undefined
      if ((kind === 'assign' || kind === 'reassign') && (!technician || technician.role !== 'TECNICO' || technician.areaId !== areaIdOf(r) || !technician.active)) {
        errors.technicianId = 'Selecciona un técnico activo del área.'
      }
      if ((kind === 'reassign' || kind === 'resolve' || kind === 'close') && reason.length < 5) {
        errors.reason = 'Indica un motivo de al menos 5 caracteres.'
      }
      if (kind === 'reject' && reason.length < 10) errors.reason = 'Indica un motivo de al menos 10 caracteres.'
      if (kind === 'close' && !body.closureReason) errors.closureReason = 'Indica si no procede o es duplicada.'
      if (Object.keys(errors).length) return problem(400, 'Datos no válidos', Object.values(errors)[0], errors)

      const from = r.status
      const actor = actorLabel(user)
      const code = codeOf(r)
      const toRequester = (text: string) => notify([r.requesterId], text, code)

      switch (kind) {
        case 'eval':
          r.status = 'EN_EVALUACION'
          pushHistory(r, { from, to: r.status, actor, reason: 'Se inició la evaluación del caso' })
          toRequester(`Tu solicitud ${code} pasó a En evaluación.`)
          break
        case 'assign':
          r.status = 'ASIGNADA'
          r.technicianId = technician!.id
          pushHistory(r, { from, to: r.status, actor, reason: `Asignada a ${technician!.fullName}` })
          notify([technician!.id], `Se te asignó la solicitud ${code}.`, code)
          toRequester(`Tu solicitud ${code} pasó a Asignada.`)
          break
        case 'reassign':
          r.technicianId = technician!.id
          pushHistory(r, { from, to: from, actor, reason: `Reasignada a ${technician!.fullName}. Motivo: ${reason}` })
          notify([technician!.id], `Se te asignó la solicitud ${code}.`, code)
          break
        case 'start':
          r.status = 'EN_ATENCION'
          pushHistory(r, { from, to: r.status, actor, reason: 'Atención iniciada en sitio' })
          toRequester(`Tu solicitud ${code} pasó a En atención.`)
          break
        case 'resolve':
          r.status = 'RESUELTA'
          r.solution = reason
          pushHistory(r, { from, to: r.status, actor, reason: `Solución: ${reason}` })
          toRequester(`Tu solicitud ${code} pasó a Resuelta. Confirma si quedó solucionada.`)
          break
        case 'confirm':
          r.status = 'CERRADA'
          r.closureReason = 'CONFIRMADA'
          pushHistory(r, { from, to: r.status, actor, reason: 'Solución confirmada por el solicitante' })
          break
        case 'reject':
          r.status = 'EN_ATENCION'
          pushHistory(r, { from, to: r.status, actor, reason: `Solución rechazada: ${reason}` })
          if (r.technicianId) notify([r.technicianId], `La solución de ${code} fue rechazada por el solicitante.`, code)
          toRequester(`Tu solicitud ${code} volvió a En atención por rechazo de la solución.`)
          break
        case 'close':
          r.status = 'CERRADA'
          r.closureReason = body.closureReason!
          pushHistory(r, {
            from,
            to: r.status,
            actor,
            reason: `${body.closureReason === 'DUPLICADA' ? 'Duplicada' : 'No procede'}: ${reason}`,
          })
          toRequester(`Tu solicitud ${code} pasó a Cerrada.`)
          break
      }
      return HttpResponse.json(detailOf(r, user))
    }),
  ),

  http.patch(
    '*/api/v1/requests/:code/priority',
    secured(['SUPERVISOR'], async ({ request, params, user }) => {
      const db = getDb()
      const r = findByCode(String(params.code))
      if (!r || !inScope(r, user)) return notFound()
      const body = (await request.json()) as ChangePriorityBody
      const priority = db.priorities.find((p) => p.id === body.priorityId)
      if (!priority) return problem(400, 'Datos no válidos', 'Prioridad inexistente.', { priorityId: 'Selecciona una prioridad.' })
      if (r.status === 'RESUELTA' || r.status === 'CERRADA') return problem(409, 'Transición inválida', 'La solicitud ya no admite cambios.')
      const previous = db.priorities.find((p) => p.id === r.priorityId)!
      r.priorityId = priority.id
      r.dueAt = computeDueAt(r.createdAt, priority.slaHours)
      pushHistory(r, {
        from: r.status,
        to: r.status,
        actor: actorLabel(user),
        reason: `Nuevo vencimiento: ${new Date(r.dueAt).toLocaleString('es-PE', { timeZone: 'America/Lima' })}`,
        note: `Prioridad ${previous.name} → Prioridad ${priority.name}`,
      })
      return HttpResponse.json(detailOf(r, user))
    }),
  ),

  http.post(
    '*/api/v1/requests/:code/comments',
    secured(null, async ({ request, params, user }) => {
      const r = findByCode(String(params.code))
      if (!r || !inScope(r, user)) return notFound()
      const body = (await request.json()) as CreateCommentBody
      const text = body.text?.trim() ?? ''
      if (!text || text.length > 1000) {
        return problem(400, 'Datos no válidos', 'El comentario debe tener entre 1 y 1000 caracteres.', { text: 'Escribe un comentario (máx. 1000 caracteres).' })
      }
      const db = getDb()
      r.comments.push({ id: ++db.nextId, authorId: user.id, authorName: user.fullName, text, createdAt: Date.now() })
      const others = [r.requesterId, r.technicianId].filter((id): id is number => id !== null && id !== user.id)
      notify(others, `Se agregó un comentario en ${codeOf(r)}.`, codeOf(r))
      return HttpResponse.json(detailOf(r, user), { status: 201 })
    }),
  ),

  http.post(
    '*/api/v1/requests/:code/rating',
    secured(['SOLICITANTE'], async ({ request, params, user }) => {
      const r = findByCode(String(params.code))
      if (!r || r.requesterId !== user.id) return notFound()
      if (r.status !== 'RESUELTA') return problem(409, 'Transición inválida', 'Solo se puede calificar una solicitud resuelta.')
      const body = (await request.json()) as RateBody
      if (!Number.isInteger(body.score) || body.score < 1 || body.score > 5) {
        return problem(400, 'Datos no válidos', 'La calificación debe ser de 1 a 5.', { score: 'Califica de 1 a 5 estrellas.' })
      }
      const comment = body.comment?.trim() || null
      r.rating = { score: body.score, comment }
      const from = r.status
      r.status = 'CERRADA'
      r.closureReason = 'CONFIRMADA'
      pushHistory(r, {
        from,
        to: r.status,
        actor: user.fullName,
        reason: `Solución confirmada · ${body.score} de 5 estrellas${comment ? `: ${comment}` : ''}`,
      })
      return HttpResponse.json(detailOf(r, user))
    }),
  ),
]
