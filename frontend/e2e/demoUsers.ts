/** Demo accounts created by backend/src/main/resources/db/seed-dev (local and staging only). */
export const DEMO_PASSWORD = 'Demo12345!'

export const demoUsers = {
  estudiante: { email: 'estudiante@unmsm.edu.pe', home: '/inicio' },
  administrador: { email: 'admin@unmsm.edu.pe', home: '/dashboard' },
} as const
