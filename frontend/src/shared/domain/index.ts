/** Tipos y constantes del dominio compartidos por todas las features (spec §4–§7). */

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

export type User = {
  id: number
  name: string
  email: string
  role: Role
  /** Área del técnico o supervisor. */
  area: string | null
  /** Solo para solicitantes. */
  type: RequesterType | null
  active: boolean
}

export type CurrentUser = Pick<User, 'id' | 'name' | 'email' | 'role' | 'area'>

export type Area = { id: number; name: string }
export type Category = { id: number; name: string; area: string }
export type PriorityLevel = { name: Priority; slaHours: number; order: number }
export type LocationLevel = 'facultad' | 'pabellon' | 'ambiente'
export type Location = { id: number; name: string; level: LocationLevel; parentId: number | null }

export type Catalogs = {
  areas: Area[]
  categories: Category[]
  priorities: PriorityLevel[]
  locations: Location[]
}

export type HistoryEntry = {
  /** Estado anterior, o `—` en el registro inicial; también "Prioridad X" en cambios de prioridad. */
  from: string
  to: string
  reason: string
  who: string
  at: number
}

export type RequestComment = { id: number; author: string; text: string; at: number; mine?: boolean }
export type Evidence = { id: number; name: string; kind: 'IMG' | 'PDF'; sizeKb: number }
export type Rating = { score: number; comment: string }

export type ServiceRequest = {
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
  history: HistoryEntry[]
  comments: RequestComment[]
  evidences: Evidence[]
  solution: string | null
  rating: Rating | null
}

export type AppNotification = {
  id: number
  recipientRole: Role
  recipientName: string | null
  text: string
  at: number
  read: boolean
  requestCode: string | null
}

export const stateIndex = (s: RequestState): number => REQUEST_STATES.indexOf(s)
export const isFinalState = (s: RequestState): boolean => stateIndex(s) >= 4
