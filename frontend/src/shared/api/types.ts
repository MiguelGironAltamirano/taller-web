/**
 * Tipos del contrato de la API (`/api/v1`).
 *
 * Base: contrato del MVP definido en `docs/plans/2026-10-16-sprint-1-mvp.md` (tarea A3).
 * Los campos marcados como EXTENSIÓN los necesita el diseño y no estaban en el contrato del MVP;
 * se documentan en `docs/api/ui-extensions.md` para acordarlos antes de implementarlos en el backend.
 */

export type Role = 'SOLICITANTE' | 'TECNICO' | 'SUPERVISOR' | 'ADMINISTRADOR'
export type RequesterType = 'ESTUDIANTE' | 'DOCENTE' | 'ADMINISTRATIVO'
export type RequestStatus = 'REGISTRADA' | 'EN_EVALUACION' | 'ASIGNADA' | 'EN_ATENCION' | 'RESUELTA' | 'CERRADA'
export type ClosureReason = 'NO_PROCEDE' | 'DUPLICADA' | 'CONFIRMADA' | 'AUTOMATICA'

export type UserSummary = { id: number; fullName: string; email: string; role: Role; areaId: number | null }
export type TokenResponse = { accessToken: string; expiresIn: number; user: UserSummary }

export type RegisterBody = { fullName: string; email: string; password: string; requesterType: RequesterType }
export type LoginBody = { email: string; password: string }
export type ChangePasswordBody = { currentPassword: string; newPassword: string }

export type LocationKind = 'FACULTAD' | 'PABELLON' | 'AMBIENTE'

export type Catalogs = {
  areas: { id: number; name: string }[]
  categories: { id: number; name: string; areaId: number }[]
  priorities: { id: number; name: string; slaHours: number }[]
  locations: { id: number; name: string; kind: LocationKind; parentId: number | null }[]
}

export type Technician = { id: number; fullName: string; activeRequests: number }

export type RequestSummary = {
  code: string
  categoryName: string
  locationName: string
  priorityName: string
  status: RequestStatus
  createdAt: string
  dueAt: string
  overdue: boolean
  // EXTENSIÓN: datos que muestran las bandejas del diseño.
  description: string
  requesterName: string
  areaName: string
  technicianName: string | null
  facultyName: string
  pavilionName: string
  roomName: string
}

export type HistoryEntry = {
  fromStatus: RequestStatus | null
  toStatus: RequestStatus
  actorName: string
  reason: string | null
  createdAt: string
  /** EXTENSIÓN: texto libre para entradas que no cambian de estado (reasignación, prioridad). */
  note?: string
}

export type WorkflowActionId = 'eval' | 'assign' | 'reassign' | 'prio' | 'noproc' | 'dup' | 'start' | 'resolve'

export type CommentDto = { id: number; authorName: string; text: string; createdAt: string }
export type EvidenceDto = { id: number; fileName: string; kind: 'IMG' | 'PDF'; sizeBytes: number }
export type RatingDto = { score: number; comment: string | null }

export type RequestDetail = RequestSummary & {
  requester: { id: number; fullName: string }
  area: { id: number; name: string }
  technician: { id: number; fullName: string } | null
  closureReason: ClosureReason | null
  history: HistoryEntry[]
  allowedTransitions: RequestStatus[]
  // EXTENSIÓN de otros módulos (workflow, collaboration, satisfaction): opcionales hasta que existan en el backend.
  allowedActions?: WorkflowActionId[]
  comments?: CommentDto[]
  evidences?: EvidenceDto[]
  solution?: string | null
  rating?: RatingDto | null
}

export type CreateRequestBody = {
  categoryId: number
  locationId: number
  description: string
  priorityId: number
  /** EXTENSIÓN: metadatos de las evidencias adjuntas (la subida real va aparte). */
  evidences?: { fileName: string; sizeBytes: number }[]
}

export type TransitionBody = {
  to: RequestStatus
  reason?: string
  technicianId?: number
  /** EXTENSIÓN: distingue "No procede" de "Duplicada" al cerrar sin resolver. */
  closureReason?: 'NO_PROCEDE' | 'DUPLICADA'
}

export type Page<T> = { items: T[]; page: number; size: number; total: number }

// ───────── EXTENSIONES del diseño ─────────

export type ChangePriorityBody = { priorityId: number }
export type CreateCommentBody = { text: string }
export type RateBody = { score: number; comment?: string }

export type NotificationDto = {
  id: number
  text: string
  createdAt: string
  read: boolean
  requestCode: string | null
}

export type TriageRuleDto = {
  id: number
  keywords: string[]
  categoryId: number
  categoryName: string
  priorityId: number
  priorityName: string
  active: boolean
}
export type TriageRuleBody = { keywords: string[]; categoryId: number; priorityId: number; active?: boolean }
export type TriageSuggestionDto = { categoryId: number; priorityId: number } | null

export type DashboardRange = '7' | '30' | 'mes'

export type DashboardIndicators = {
  rangeDays: number
  areaName: string | null
  kpis: {
    registered: KpiValue
    pending: KpiValue
    attended: KpiValue
    resolvedPct: KpiValue
    avgAttentionHours: KpiValue
    overdue: KpiValue
  }
  byCategory: { label: string; value: number }[]
  byPriority: { label: string; value: number }[]
  byTechnician: { label: string; value: number }[]
  timeline: number[]
  slaCompliancePct: number
  dueSoon: RequestSummary[]
}
export type KpiValue = { value: number; deltaPct: number; series: number[] }

export type TeamMemberLoad = { id: number; fullName: string; assigned: number; inProgress: number; overdue: number; capacity: number }

export type AdminUser = {
  id: number
  fullName: string
  email: string
  role: Role
  areaId: number | null
  areaName: string | null
  requesterType: RequesterType | null
  active: boolean
  /** Solo en la respuesta de `POST /admin/users`: se muestra una vez y no se vuelve a enviar. */
  temporaryPassword?: string
}
export type AdminUserBody = { fullName: string; email: string; role: Role; areaId: number | null; requesterType?: RequesterType }

export type AuditEvent = { id: number; createdAt: string; actorName: string; requestCode: string; detail: string }

export type AppConfig = { autoCloseHours: number; emailNotifications: boolean; triageSuggestions: boolean }

export type StateInfo = { order: number; status: RequestStatus; description: string }
