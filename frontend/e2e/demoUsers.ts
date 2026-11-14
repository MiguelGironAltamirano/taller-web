/**
 * Demo accounts created by backend/src/main/resources/db/seed-dev and db/seed-demo (local and staging only).
 * Every account shares the same password.
 */
export const DEMO_PASSWORD = 'Demo12345!'

/** Accounts used by the login smoke test (role → landing page). */
export const demoUsers = {
  estudiante: { email: 'estudiante@unmsm.edu.pe', home: '/inicio' },
  administrador: { email: 'admin@unmsm.edu.pe', home: '/dashboard' },
} as const

/** Accounts used by the journeys. Mutating journeys stay in the Mantenimiento area (id 1). */
export const accounts = {
  estudiante: { email: 'estudiante@unmsm.edu.pe', name: 'Estudiante Demo', home: '/inicio' },
  docente: { email: 'docente@unmsm.edu.pe', name: 'Docente Demo', home: '/inicio' },
  supervisor: { email: 'supervisor.mantenimiento@unmsm.edu.pe', name: 'Supervisora Mantenimiento', home: '/dashboard' },
  /** Read-only journeys (dashboard, team): no spec writes requests in Servicios Administrativos (area 4). */
  supervisorAdministrativo: { email: 'supervisor.administrativo@unmsm.edu.pe', name: 'Supervisor Servicios Administrativos', home: '/dashboard' },
  tecnico: { email: 'tecnico.mantenimiento@unmsm.edu.pe', name: 'Técnico Mantenimiento', home: '/solicitudes' },
  admin: { email: 'admin@unmsm.edu.pe', name: 'Administrador Demo', home: '/dashboard' },
} as const

export type Account = { email: string; name: string; home: string }
