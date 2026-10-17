import {
  PRIORITIES,
  REQUEST_STATES,
  type NotificationItem,
  type Priority,
  type RequestDetailView,
  type RequestRow,
  type RequestState,
  type RequesterType,
  type Role,
  type SessionUser,
} from '../domain'
import type {
  Catalogs,
  NotificationDto,
  RequestDetail,
  RequestStatus,
  RequestSummary,
  RequesterType as ApiRequesterType,
  Role as ApiRole,
  UserSummary,
} from './types'

export const STATUS_ORDER: RequestStatus[] = [
  'REGISTRADA',
  'EN_EVALUACION',
  'ASIGNADA',
  'EN_ATENCION',
  'RESUELTA',
  'CERRADA',
]

export const statusToState = (s: RequestStatus): RequestState => REQUEST_STATES[STATUS_ORDER.indexOf(s)]
export const stateToStatus = (s: RequestState): RequestStatus => STATUS_ORDER[REQUEST_STATES.indexOf(s)]

export const roleFromApi = (r: ApiRole): Role => r.toLowerCase() as Role
export const roleToApi = (r: Role): ApiRole => r.toUpperCase() as ApiRole

const REQUESTER_LABEL: Record<ApiRequesterType, RequesterType> = {
  ESTUDIANTE: 'Estudiante',
  DOCENTE: 'Docente',
  ADMINISTRATIVO: 'Administrativo',
}
export const requesterTypeFromApi = (t: ApiRequesterType): RequesterType => REQUESTER_LABEL[t]
export const requesterTypeToApi = (t: RequesterType): ApiRequesterType =>
  t.toUpperCase() as ApiRequesterType

function asPriority(name: string): Priority {
  return (PRIORITIES as readonly string[]).includes(name) ? (name as Priority) : 'Media'
}

export function toSessionUser(u: UserSummary, catalogs?: Catalogs): SessionUser & { areaName: string | null } {
  return {
    id: u.id,
    name: u.fullName,
    email: u.email,
    role: roleFromApi(u.role),
    areaId: u.areaId,
    areaName: u.areaId == null ? null : (catalogs?.areas.find((a) => a.id === u.areaId)?.name ?? null),
  }
}

export function toRequestRow(s: RequestSummary): RequestRow {
  return {
    code: s.code,
    requester: s.requesterName,
    category: s.categoryName,
    area: s.areaName,
    faculty: s.facultyName,
    pavilion: s.pavilionName,
    room: s.roomName,
    description: s.description,
    priority: asPriority(s.priorityName),
    state: statusToState(s.status),
    technician: s.technicianName,
    createdAt: Date.parse(s.createdAt),
    dueAt: Date.parse(s.dueAt),
  }
}

export function toRequestDetail(d: RequestDetail): RequestDetailView {
  return {
    ...toRequestRow(d),
    requesterId: d.requester.id,
    technicianId: d.technician?.id ?? null,
    history: d.history.map((h) => ({
      from: h.note ? h.note.split('→')[0].trim() : h.fromStatus ? statusToState(h.fromStatus) : '—',
      to: h.note ? (h.note.split('→')[1] ?? '').trim() : statusToState(h.toStatus),
      reason: h.reason ?? '',
      who: h.actorName,
      at: Date.parse(h.createdAt),
    })),
    comments: d.comments.map((c) => ({ id: c.id, author: c.authorName, text: c.text, at: Date.parse(c.createdAt) })),
    evidences: d.evidences.map((e) => ({ id: e.id, name: e.fileName, kind: e.kind, sizeKb: Math.round(e.sizeBytes / 1024) })),
    solution: d.solution,
    rating: d.rating ? { score: d.rating.score, comment: d.rating.comment ?? '' } : null,
    allowedActions: d.allowedActions,
  }
}

export function toNotificationItem(n: NotificationDto): NotificationItem {
  return { id: n.id, text: n.text, at: Date.parse(n.createdAt), read: n.read, requestCode: n.requestCode }
}
