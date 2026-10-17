import { HttpResponse } from 'msw'
import type { Role } from '../shared/api/types'
import { getDb, type MockUser } from './db'

/** Respuesta de error RFC 7807 (`application/problem+json`), igual que el backend. */
export function problem(status: number, title: string, detail?: string, errors?: Record<string, string>) {
  return HttpResponse.json(
    { type: 'about:blank', title, status, detail, errors },
    { status, headers: { 'Content-Type': 'application/problem+json' } },
  )
}

export const unauthorized = () => problem(401, 'No autenticado', 'Inicia sesión para continuar.')
export const forbidden = () => problem(403, 'Sin permiso', 'No tienes permiso para realizar esta acción.')
export const notFound = (detail = 'El recurso no existe o no es visible para ti.') => problem(404, 'No encontrado', detail)

const TOKEN_PREFIX = 'mock-token-'
export const tokenFor = (user: MockUser) => `${TOKEN_PREFIX}${user.id}`

/** Resuelve el usuario autenticado a partir de la cabecera `Authorization: Bearer …`. */
export function authenticate(request: Request): MockUser | null {
  const header = request.headers.get('Authorization') ?? ''
  if (!header.startsWith(`Bearer ${TOKEN_PREFIX}`)) return null
  const id = Number(header.slice(`Bearer ${TOKEN_PREFIX}`.length))
  const user = getDb().users.find((u) => u.id === id)
  return user?.active ? user : null
}

type Ctx = { user: MockUser; request: Request; params: Record<string, string | readonly string[] | undefined> }

/**
 * Envuelve un handler exigiendo sesión (401) y, opcionalmente, un rol (403).
 */
export function secured<A extends { request: Request; params: Ctx['params'] }>(
  roles: Role[] | null,
  handler: (ctx: Ctx & A) => Response | Promise<Response>,
) {
  return (args: A) => {
    const user = authenticate(args.request)
    if (!user) return unauthorized()
    if (roles && !roles.includes(user.role)) return forbidden()
    return handler({ ...args, user })
  }
}

export const nowMs = () => Date.now()
export const iso = (ms: number) => new Date(ms).toISOString()
