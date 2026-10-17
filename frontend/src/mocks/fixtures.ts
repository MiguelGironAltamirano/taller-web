import type { Catalogs, Role, UserSummary } from '../shared/api/types'

export const catalogs: Catalogs = {
  areas: [
    { id: 1, name: 'Mantenimiento' },
    { id: 2, name: 'Infraestructura' },
    { id: 3, name: 'Soporte Informático' },
    { id: 4, name: 'Servicios Administrativos' },
  ],
  categories: [
    { id: 1, name: 'Electricidad', areaId: 1 },
    { id: 2, name: 'Gasfitería', areaId: 1 },
    { id: 3, name: 'Mobiliario', areaId: 2 },
    { id: 4, name: 'Aulas y laboratorios', areaId: 2 },
    { id: 5, name: 'Equipos de cómputo', areaId: 3 },
    { id: 6, name: 'Red e Internet', areaId: 3 },
    { id: 7, name: 'Trámites administrativos', areaId: 4 },
  ],
  priorities: [
    { id: 1, name: 'Crítica', slaHours: 8 },
    { id: 2, name: 'Alta', slaHours: 24 },
    { id: 3, name: 'Media', slaHours: 72 },
    { id: 4, name: 'Baja', slaHours: 120 },
  ],
  locations: [
    { id: 1, name: 'Facultad de Ingeniería de Sistemas e Informática', kind: 'FACULTAD', parentId: null },
    { id: 2, name: 'Pabellón Principal', kind: 'PABELLON', parentId: 1 },
    { id: 3, name: 'Aula 101', kind: 'AMBIENTE', parentId: 2 },
    { id: 4, name: 'Laboratorio 2', kind: 'AMBIENTE', parentId: 2 },
    { id: 5, name: 'Facultad de Ciencias Matemáticas', kind: 'FACULTAD', parentId: null },
    { id: 6, name: 'Pabellón A', kind: 'PABELLON', parentId: 5 },
    { id: 7, name: 'Aula 201', kind: 'AMBIENTE', parentId: 6 },
    { id: 8, name: 'Biblioteca', kind: 'AMBIENTE', parentId: 6 },
  ],
}

export const demoPassword = 'Demo12345!'

export const users: UserSummary[] = [
  { id: 1, fullName: 'Administrador Demo', email: 'admin@unmsm.edu.pe', role: 'ADMINISTRADOR', areaId: null },
  { id: 2, fullName: 'Supervisora Mantenimiento', email: 'supervisor.mantenimiento@unmsm.edu.pe', role: 'SUPERVISOR', areaId: 1 },
  { id: 3, fullName: 'Técnico Mantenimiento', email: 'tecnico.mantenimiento@unmsm.edu.pe', role: 'TECNICO', areaId: 1 },
  { id: 4, fullName: 'Estudiante Demo', email: 'estudiante@unmsm.edu.pe', role: 'SOLICITANTE', areaId: null },
]

export function tokenFor(user: UserSummary): string {
  return `mock-token-${user.id}`
}

export function userFromToken(header: string | null): UserSummary | undefined {
  const id = Number(header?.replace('Bearer mock-token-', ''))
  return users.find((user) => user.id === id)
}

export function roleIs(user: UserSummary, ...roles: Role[]): boolean {
  return roles.includes(user.role)
}
