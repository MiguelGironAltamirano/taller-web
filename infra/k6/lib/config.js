// Shared settings of the load tests (docs/calidad/plan-de-pruebas.md §6).

/** Entry point: nginx of the stack under test. Inside the Compose network it is the "frontend" service. */
export const BASE_URL = (__ENV.BASE_URL || 'http://frontend').replace(/\/$/, '')
export const API = `${BASE_URL}/api/v1`

/** Password of the demo accounts (db/seed-dev). Load stacks only: never point these scripts at production. */
export const DEMO_PASSWORD = __ENV.DEMO_PASSWORD || 'Demo12345!'

/**
 * Accounts whose token the inbox scenarios share. One login per account in setup(): a user has a single open
 * session, so logging in again during the test would close the session the virtual users are using. Several
 * virtual users may share one token (it is the same session), which mirrors several tabs of one browser.
 */
export const BROWSING_ACCOUNTS = [
  { email: 'admin@unmsm.edu.pe', role: 'ADMINISTRADOR' },
  { email: 'supervisor.mantenimiento@unmsm.edu.pe', role: 'SUPERVISOR' },
  { email: 'supervisor.infraestructura@unmsm.edu.pe', role: 'SUPERVISOR' },
  { email: 'supervisora.informatica@unmsm.edu.pe', role: 'SUPERVISOR' },
  { email: 'supervisor.administrativo@unmsm.edu.pe', role: 'SUPERVISOR' },
  { email: 'tecnico.mantenimiento@unmsm.edu.pe', role: 'TECNICO' },
  { email: 'tecnico.infraestructura@unmsm.edu.pe', role: 'TECNICO' },
  { email: 'tecnico.informatica@unmsm.edu.pe', role: 'TECNICO' },
  { email: 'tecnica.administrativa@unmsm.edu.pe', role: 'TECNICO' },
  { email: 'estudiante@unmsm.edu.pe', role: 'SOLICITANTE' },
  { email: 'estudiante2@unmsm.edu.pe', role: 'SOLICITANTE' },
  { email: 'docente@unmsm.edu.pe', role: 'SOLICITANTE' },
  { email: 'administrativo@unmsm.edu.pe', role: 'SOLICITANTE' },
]

export const JSON_HEADERS = { 'Content-Type': 'application/json', Accept: 'application/json' }

/** Uniform think time between `min` and `max` seconds: people read a page before clicking again. */
export function think(min, max) {
  return min + Math.random() * (max - min)
}

export function pick(list) {
  return list[Math.floor(Math.random() * list.length)]
}
