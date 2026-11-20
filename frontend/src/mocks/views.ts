import { statusToState } from '../shared/api/mappers'
import type { RequestDetail, RequestStatus, RequestSummary, WorkflowActionId } from '../shared/api/types'
import { availableActions } from '../features/workflow/lib/transitions'
import { slaStatus } from '../features/workflow/lib/sla'
import { codeOf, getDb, type MockRequest, type MockUser } from './db'
import { iso } from './http'

/** Nombre del rol en minúsculas como lo usa el dominio de la UI. */
const roleKey = (u: MockUser) => u.role.toLowerCase() as 'solicitante' | 'tecnico' | 'supervisor' | 'administrador'

export function areaIdOf(r: MockRequest): number {
  return getDb().categories.find((c) => c.id === r.categoryId)!.areaId
}

/** Visibilidad según el rol (spec §5): propias, asignadas, del área, o todas. */
export function inScope(r: MockRequest, user: MockUser): boolean {
  switch (user.role) {
    case 'SOLICITANTE':
      return r.requesterId === user.id
    case 'TECNICO':
      return r.technicianId === user.id
    case 'SUPERVISOR':
      return areaIdOf(r) === user.areaId
    default:
      return true
  }
}

/** Días durante los que el solicitante puede restaurar una solicitud cancelada (como el backend). */
export const RESTORE_DAYS = 7

/** La última cancelación según el historial: el estado que tenía y cuándo. */
export function lastCancellation(r: MockRequest): { previous: RequestStatus; at: number } | null {
  const entry = [...r.history].reverse().find((h) => h.toStatus === 'CANCELADA' && h.fromStatus)
  return entry ? { previous: entry.fromStatus!, at: entry.createdAt } : null
}

export function restorableUntil(r: MockRequest): number | null {
  const c = lastCancellation(r)
  return c ? c.at + RESTORE_DAYS * 86_400_000 : null
}

export function allowedActionsFor(r: MockRequest, user: MockUser): WorkflowActionId[] {
  if (user.role === 'SOLICITANTE') {
    if (r.requesterId !== user.id) return []
    if (r.status === 'REGISTRADA' || r.status === 'EN_EVALUACION') return ['cancel']
    const until = restorableUntil(r)
    return r.status === 'CANCELADA' && until !== null && Date.now() <= until ? ['restore'] : []
  }
  if (user.role === 'SUPERVISOR' && areaIdOf(r) !== user.areaId) return []
  if (user.role === 'TECNICO' && r.technicianId !== user.id) return []
  return availableActions(roleKey(user), statusToState(r.status))
}

const ACTION_TARGET: Record<WorkflowActionId, RequestStatus | null> = {
  eval: 'EN_EVALUACION',
  assign: 'ASIGNADA',
  reassign: 'ASIGNADA',
  start: 'EN_ATENCION',
  resolve: 'RESUELTA',
  noproc: 'CERRADA',
  dup: 'CERRADA',
  prio: null,
  cancel: 'CANCELADA',
  restore: null,
}

export function allowedTransitionsFor(r: MockRequest, user: MockUser): RequestStatus[] {
  if (user.role === 'SOLICITANTE' && r.requesterId === user.id && r.status === 'RESUELTA') {
    return ['CERRADA', 'EN_ATENCION']
  }
  if (user.role === 'SOLICITANTE' && r.status === 'CANCELADA' && allowedActionsFor(r, user).includes('restore')) {
    return [lastCancellation(r)!.previous]
  }
  const targets = allowedActionsFor(r, user)
    .map((a) => ACTION_TARGET[a])
    .filter((t): t is RequestStatus => t !== null)
  return [...new Set(targets)]
}

export function summaryOf(r: MockRequest, now = Date.now()): RequestSummary {
  const db = getDb()
  const category = db.categories.find((c) => c.id === r.categoryId)!
  const priority = db.priorities.find((p) => p.id === r.priorityId)!
  const room = db.locations.find((l) => l.id === r.locationId)!
  const pavilion = db.locations.find((l) => l.id === room.parentId)!
  const faculty = db.locations.find((l) => l.id === pavilion.parentId)!
  const requester = db.users.find((u) => u.id === r.requesterId)!
  const technician = db.users.find((u) => u.id === r.technicianId)
  const area = db.areas.find((a) => a.id === category.areaId)!
  const sla = slaStatus({ state: statusToState(r.status), createdAt: r.createdAt, dueAt: r.dueAt }, now)
  return {
    code: codeOf(r),
    categoryName: category.name,
    locationName: `${faculty.name} → ${pavilion.name} → ${room.name}`,
    priorityName: priority.name,
    status: r.status,
    createdAt: iso(r.createdAt),
    dueAt: iso(r.dueAt),
    overdue: sla.overdue,
    description: r.description,
    requesterName: requester.fullName,
    areaName: area.name,
    technicianName: technician?.fullName ?? null,
    facultyName: faculty.name,
    pavilionName: pavilion.name,
    roomName: room.name,
  }
}

export function detailOf(r: MockRequest, user: MockUser, now = Date.now()): RequestDetail {
  const db = getDb()
  const base = summaryOf(r, now)
  const category = db.categories.find((c) => c.id === r.categoryId)!
  const area = db.areas.find((a) => a.id === category.areaId)!
  const technician = db.users.find((u) => u.id === r.technicianId)
  return {
    ...base,
    requester: { id: r.requesterId, fullName: base.requesterName },
    area: { id: area.id, name: area.name },
    technician: technician ? { id: technician.id, fullName: technician.fullName } : null,
    closureReason: r.closureReason,
    history: r.history.map((h) => ({ ...h, createdAt: iso(h.createdAt) })),
    allowedTransitions: allowedTransitionsFor(r, user),
    allowedActions: allowedActionsFor(r, user),
    comments: r.comments.map((c) => ({ id: c.id, authorName: c.authorName, text: c.text, createdAt: iso(c.createdAt) })),
    evidences: r.evidences,
    solution: r.solution,
    rating: r.rating,
    autoCloseAt: autoCloseAtOf(r),
  }
}

/** Como el backend: última entrada a RESUELTA + `autoCloseHours` vigente; null si no está Resuelta. */
function autoCloseAtOf(r: MockRequest): string | null {
  if (r.status !== 'RESUELTA') return null
  const resolvedAt = r.history
    .filter((h) => h.toStatus === 'RESUELTA' && h.fromStatus !== 'RESUELTA')
    .reduce<number | null>((latest, h) => (latest === null || h.createdAt > latest ? h.createdAt : latest), null)
  return resolvedAt === null ? null : iso(resolvedAt + getDb().config.autoCloseHours * 3_600_000)
}

export function findByCode(code: string): MockRequest | undefined {
  return getDb().requests.find((r) => codeOf(r) === code)
}

export function notify(recipientIds: number[], text: string, requestCode: string | null) {
  const db = getDb()
  for (const recipientId of recipientIds) {
    db.notifications.unshift({
      id: ++db.nextId,
      recipientId,
      text,
      createdAt: Date.now(),
      read: false,
      requestCode,
    })
  }
}
