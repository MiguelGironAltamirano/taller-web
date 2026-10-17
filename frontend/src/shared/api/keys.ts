/**
 * Claves de TanStack Query compartidas. Viven aquí para que una feature pueda invalidar los datos
 * de otra (p. ej. una transición de estado refresca solicitudes y notificaciones) sin importarla.
 */
export const requestsKey = ['requests'] as const
export const requestListKey = ['requests', 'list'] as const
export const requestDetailKey = (code: string) => ['requests', 'detail', code] as const
export const notificationsKey = ['notifications'] as const
export const dashboardKey = ['dashboard'] as const
export const triageRulesKey = ['admin', 'triage-rules'] as const
export const adminKey = ['admin'] as const
