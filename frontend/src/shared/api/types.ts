export type Role = 'SOLICITANTE' | 'TECNICO' | 'SUPERVISOR' | 'ADMINISTRADOR'
export type RequesterType = 'ESTUDIANTE' | 'DOCENTE' | 'ADMINISTRATIVO'
export type RequestStatus = 'REGISTRADA' | 'EN_EVALUACION' | 'ASIGNADA' | 'EN_ATENCION' | 'RESUELTA' | 'CERRADA'
export type ClosureReason = 'NO_PROCEDE' | 'DUPLICADA' | 'CONFIRMADA'

export type UserSummary = { id: number; fullName: string; email: string; role: Role; areaId: number | null }
export type TokenResponse = { accessToken: string; expiresIn: number; user: UserSummary }

export type RegisterBody = { fullName: string; email: string; password: string; requesterType: RequesterType }
export type LoginBody = { email: string; password: string }

export type Catalogs = {
  areas: { id: number; name: string }[]
  categories: { id: number; name: string; areaId: number }[]
  priorities: { id: number; name: string; slaHours: number }[]
  locations: { id: number; name: string; kind: 'FACULTAD' | 'PABELLON' | 'AMBIENTE'; parentId: number | null }[]
}

export type Technician = { id: number; fullName: string }

export type RequestSummary = {
  code: string
  categoryName: string
  locationName: string
  priorityName: string
  status: RequestStatus
  createdAt: string
  dueAt: string
  overdue: boolean
}

export type HistoryEntry = {
  fromStatus: RequestStatus | null
  toStatus: RequestStatus
  actorName: string
  reason: string | null
  createdAt: string
}

export type RequestDetail = RequestSummary & {
  description: string
  requester: { id: number; fullName: string }
  area: { id: number; name: string }
  technician: { id: number; fullName: string } | null
  closureReason: ClosureReason | null
  history: HistoryEntry[]
  allowedTransitions: RequestStatus[]
}

export type CreateRequestBody = { categoryId: number; locationId: number; description: string; priorityId: number }
export type TransitionBody = { to: RequestStatus; reason?: string; technicianId?: number }

export type Page<T> = { items: T[]; page: number; size: number; total: number }
