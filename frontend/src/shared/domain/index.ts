/**
 * Modelo de vista del dominio (spec §4–§7): etiquetas en español y fechas en milisegundos.
 * La API habla con los códigos del contrato (`EN_ATENCION`, ISO-8601); `shared/api/mappers.ts` traduce.
 */

export const REQUEST_STATES = [
  'Registrada',
  'En evaluación',
  'Asignada',
  'En atención',
  'Resuelta',
  'Cerrada',
] as const
export type RequestState = (typeof REQUEST_STATES)[number]

export const PRIORITIES = ['Crítica', 'Alta', 'Media', 'Baja'] as const
export type Priority = (typeof PRIORITIES)[number]

/** Horas de SLA iniciales por prioridad (spec §5.1); el administrador puede editarlas. */
export const DEFAULT_SLA_HOURS: Record<Priority, number> = {
  Crítica: 8,
  Alta: 24,
  Media: 72,
  Baja: 120,
}

export const ROLES = ['solicitante', 'tecnico', 'supervisor', 'administrador'] as const
export type Role = (typeof ROLES)[number]

export const ROLE_LABELS: Record<Role, string> = {
  solicitante: 'Solicitante',
  tecnico: 'Técnico',
  supervisor: 'Supervisor',
  administrador: 'Administrador',
}

export const REQUESTER_TYPES = ['Estudiante', 'Docente', 'Administrativo'] as const
export type RequesterType = (typeof REQUESTER_TYPES)[number]

export const INSTITUTIONAL_DOMAIN = 'unmsm.edu.pe'

export type SessionUser = {
  id: number
  name: string
  email: string
  role: Role
  areaId: number | null
}

export type WorkflowActionId = 'eval' | 'assign' | 'reassign' | 'prio' | 'noproc' | 'dup' | 'start' | 'resolve'

/** Fila de una bandeja: lo necesario para listar, filtrar y calcular el SLA. */
export type RequestRow = {
  code: string
  requester: string
  category: string
  area: string
  faculty: string
  pavilion: string
  room: string
  description: string
  priority: Priority
  state: RequestState
  technician: string | null
  createdAt: number
  dueAt: number
}

export type HistoryItem = {
  /** Estado anterior, `—` en el registro inicial, o una nota (p. ej. "Prioridad Alta"). */
  from: string
  to: string
  reason: string
  who: string
  at: number
}

export type CommentItem = { id: number; author: string; text: string; at: number }
export type EvidenceItem = { id: number; name: string; kind: 'IMG' | 'PDF'; sizeKb: number }

export type RequestDetailView = RequestRow & {
  requesterId: number
  technicianId: number | null
  history: HistoryItem[]
  comments: CommentItem[]
  evidences: EvidenceItem[]
  solution: string | null
  rating: { score: number; comment: string } | null
  allowedActions: WorkflowActionId[]
}

export type NotificationItem = {
  id: number
  text: string
  at: number
  read: boolean
  requestCode: string | null
}

export const stateIndex = (s: RequestState): number => REQUEST_STATES.indexOf(s)
export const isFinalState = (s: RequestState): boolean => stateIndex(s) >= 4
