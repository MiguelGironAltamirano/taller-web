import { http, HttpResponse } from 'msw'
import type {
  ChangePriorityBody,
  CreateCommentBody,
  CreateRequestBody,
  RateBody,
  RequestStatus,
  TransitionBody,
} from '../../shared/api/types'
import { RATING_COMMENT_MAX } from '../../shared/api/types'
import { statusToState } from '../../shared/api/mappers'
import { computeDueAt } from '../../features/workflow/lib/sla'
import { codeOf, getDb, type MockRequest, type MockUser } from '../db'
import { forbidden, notFound, problem, secured } from '../http'
import { countRequests, listRequests, parseListQuery } from '../requestList'
import { areaIdOf, detailOf, findByCode, inScope, notify } from '../views'

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
  entry: {
    from: RequestStatus | null
    to: RequestStatus
    actor: string
    reason: string | null
    note?: string
    internal?: boolean
  },
) =>
  r.history.push({
    fromStatus: entry.from,
    toStatus: entry.to,
    actorName: entry.actor,
    reason: entry.reason,
    createdAt: Date.now(),
    note: entry.note,
    internal: entry.internal,
  })

export const requestHandlers = [
  http.get(
    '*/api/v1/requests',
    secured(null, ({ request, user }) => {
      const parsed = parseListQuery(new URL(request.url).searchParams)
      if ('errors' in parsed) return problem(400, 'Bad Request', 'La solicitud contiene datos inválidos', parsed.errors)
      return HttpResponse.json(listRequests(user, parsed.query))
    }),
  ),

  // Antes de `/requests/:code` para que "counts" no se tome como un código.
  http.get(
    '*/api/v1/requests/counts',
    secured(null, ({ user }) => HttpResponse.json(countRequests(user))),
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
        // Evidences arrive afterwards through POST /requests/{code}/evidences, as in the real API.
        evidences: [],
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
          // Como en el backend: la solución rechazada deja de ser la propuesta.
          r.solution = null
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
      // Mismo orden que el backend: 409 (resuelta o cerrada) antes que 400 (prioridad inválida o sin cambio).
      if (r.status === 'RESUELTA' || r.status === 'CERRADA') return problem(409, 'Transición inválida', 'La solicitud ya no admite cambios.')
      const priority = db.priorities.find((p) => p.id === body.priorityId)
      if (!priority) return problem(400, 'Datos no válidos', 'Prioridad inexistente.', { priorityId: 'Selecciona una prioridad.' })
      if (priority.id === r.priorityId) {
        return problem(400, 'Datos no válidos', 'La prioridad no cambió.', { priorityId: 'Elige una prioridad distinta de la actual' })
      }
      const previous = db.priorities.find((p) => p.id === r.priorityId)!
      r.requestedPriorityId ??= r.priorityId
      r.requestedDueAt ??= r.dueAt
      r.priorityId = priority.id
      r.dueAt = computeDueAt(r.createdAt, priority.slaHours)
      pushHistory(r, {
        from: r.status,
        to: r.status,
        actor: actorLabel(user),
        reason: `Nuevo vencimiento: ${new Date(r.dueAt).toLocaleString('es-PE', { timeZone: 'America/Lima' })}`,
        note: `Prioridad ${previous.name} → Prioridad ${priority.name}`,
        internal: true,
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
      const id = ++db.nextId
      r.comments.push({ id, authorId: user.id, authorName: user.fullName, text, createdAt: Date.now() })
      const others = [r.requesterId, r.technicianId].filter((uid): uid is number => uid !== null && uid !== user.id)
      notify(others, `Se agregó un comentario en ${codeOf(r)}.`, codeOf(r))
      // Devuelve solo el CommentDto creado (contrato v2, 2026-10-17).
      return HttpResponse.json(
        { id, authorName: user.fullName, text, createdAt: new Date().toISOString() },
        { status: 201 },
      )
    }),
  ),

  http.get(
    '*/api/v1/requests/:code/comments',
    secured(null, ({ params, user }) => {
      const r = findByCode(String(params.code))
      if (!r || !inScope(r, user)) return notFound()
      const items = r.comments.map((c) => ({
        id: c.id,
        authorName: c.authorName,
        text: c.text,
        createdAt: new Date(c.createdAt).toISOString(),
      }))
      return HttpResponse.json(items)
    }),
  ),

  http.get(
    '*/api/v1/requests/:code/evidences',
    secured(null, ({ params, user }) => {
      const r = findByCode(String(params.code))
      if (!r || !inScope(r, user)) return notFound()
      return HttpResponse.json(r.evidences)
    }),
  ),

  http.post(
    '*/api/v1/requests/:code/evidences',
    secured(null, async ({ request, params, user }) => {
      const r = findByCode(String(params.code))
      if (!r || !inScope(r, user)) return notFound()
      const file = (await request.formData()).get('file')
      if (!(file instanceof File) || !/\.(jpe?g|png|pdf)$/i.test(file.name)) {
        return problem(400, 'Datos no válidos', 'Solo se permiten JPG, PNG o PDF.', { file: 'Solo se permiten JPG, PNG o PDF.' })
      }
      if (r.evidences.length >= 5) return problem(409, 'Límite alcanzado', 'La solicitud ya tiene 5 evidencias.')
      const evidence = {
        id: ++getDb().nextId,
        fileName: file.name,
        kind: /\.pdf$/i.test(file.name) ? ('PDF' as const) : ('IMG' as const),
        sizeBytes: file.size,
      }
      r.evidences.push(evidence)
      return HttpResponse.json(evidence, { status: 201 })
    }),
  ),

  http.post(
    '*/api/v1/requests/:code/rating',
    secured(['SOLICITANTE'], async ({ request, params, user }) => {
      const r = findByCode(String(params.code))
      if (!r || r.requesterId !== user.id) return notFound()
      // La calificación es opcional y se guarda después de confirmar la solución (que ya cerró la solicitud).
      if (r.status !== 'CERRADA' || r.closureReason !== 'CONFIRMADA') {
        return problem(409, 'Conflict', 'Solo se puede calificar una solicitud cuya solución confirmaste')
      }
      if (r.rating) return problem(409, 'Conflict', 'Ya calificaste esta solicitud')
      // Mismos mensajes que el backend (Rating.give): un error por campo, ambos a la vez si los dos fallan.
      const body = (await request.json()) as RateBody
      const errors: Record<string, string> = {}
      if (!Number.isInteger(body.score) || body.score < 1 || body.score > 5) errors.score = 'Califica de 1 a 5 estrellas'
      if ((body.comment?.trim().length ?? 0) > RATING_COMMENT_MAX) {
        errors.comment = `El comentario no puede superar los ${RATING_COMMENT_MAX} caracteres`
      }
      if (Object.keys(errors).length) return problem(400, 'Bad Request', 'La calificación contiene datos inválidos', errors)
      r.rating = { score: body.score, comment: body.comment?.trim() || null }
      return HttpResponse.json(detailOf(r, user))
    }),
  ),
]
