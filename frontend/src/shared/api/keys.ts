/**
 * Claves de TanStack Query compartidas. Viven aquí para que una feature pueda invalidar los datos
 * de otra (p. ej. una transición de estado refresca solicitudes y notificaciones) sin importarla.
 */
export const requestsKey = ['requests'] as const
/** Borrador de «Nueva solicitud»: fuera de `requestsKey`, que se invalida tras cada cambio de una solicitud. */
export const requestDraftKey = ['me', 'request-draft'] as const
export const requestListKey = ['requests', 'list'] as const
export const requestDetailKey = (code: string) => ['requests', 'detail', code] as const
export const notificationsKey = ['notifications'] as const
export const dashboardKey = ['dashboard'] as const
export const triageRulesKey = ['admin', 'triage-rules'] as const
export const adminKey = ['admin'] as const
export const commentsKey = (code: string) => ['requests', code, 'comments'] as const
export const evidencesKey = (code: string) => ['requests', code, 'evidences'] as const
/** Una página del listado con sus filtros; cuelga de `requestsKey` para que las mutaciones la invaliden. */
export const requestPageKey = (query: object) => ['requests', 'page', query] as const
export const requestCountsKey = ['requests', 'counts'] as const
