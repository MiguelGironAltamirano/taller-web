import { sessionStore } from '../session/session-store'
import type { TokenResponse } from './types'

const API_BASE = import.meta.env.VITE_API_BASE ?? '/api/v1'

type ProblemDetails = {
  title?: string
  status?: number
  detail?: string
  errors?: Record<string, string>
}

export class ApiError extends Error {
  readonly status: number
  readonly title: string
  readonly detail?: string
  /** Mensajes de validación por campo (`400`), si el servidor los envía. */
  readonly fieldErrors?: Record<string, string>

  constructor(status: number, title: string, detail?: string, fieldErrors?: Record<string, string>) {
    super(detail ?? title)
    this.name = 'ApiError'
    this.status = status
    this.title = title
    this.detail = detail
    this.fieldErrors = fieldErrors
  }
}

type Method = 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE'
export type QueryParams = Record<string, string | number | boolean | null | undefined>

function toUrl(path: string, query?: QueryParams): string {
  const url = new URL(`${API_BASE}${path}`, window.location.origin)
  for (const [key, value] of Object.entries(query ?? {})) {
    if (value !== undefined && value !== null && value !== '') url.searchParams.set(key, String(value))
  }
  return url.toString()
}

function send(method: Method, path: string, body?: unknown, query?: QueryParams): Promise<Response> {
  const headers: Record<string, string> = { Accept: 'application/json' }
  const session = sessionStore.get()
  if (session) headers.Authorization = `Bearer ${session.accessToken}`
  if (body !== undefined) headers['Content-Type'] = 'application/json'
  return fetch(toUrl(path, query), {
    method,
    headers,
    credentials: 'include',
    body: body === undefined ? undefined : JSON.stringify(body),
  })
}

let refreshing: Promise<boolean> | null = null

/**
 * Restores the session from the refresh cookie. Concurrent callers share one request,
 * because the server rotates the refresh token on every use.
 */
export function refreshSession(): Promise<boolean> {
  refreshing ??= (async () => {
    try {
      const response = await send('POST', '/auth/refresh')
      if (!response.ok) {
        sessionStore.clear()
        return false
      }
      const token = (await response.json()) as TokenResponse
      sessionStore.set({ accessToken: token.accessToken, user: token.user })
      return true
    } catch {
      sessionStore.clear()
      return false
    } finally {
      refreshing = null
    }
  })()
  return refreshing
}

async function toError(response: Response): Promise<ApiError> {
  const contentType = response.headers.get('Content-Type') ?? ''
  if (contentType.includes('application/problem+json')) {
    const problem = (await response.json()) as ProblemDetails
    return new ApiError(response.status, problem.title ?? response.statusText, problem.detail, problem.errors)
  }
  return new ApiError(response.status, response.statusText)
}

async function request<T>(method: Method, path: string, body?: unknown, query?: QueryParams): Promise<T> {
  let response = await send(method, path, body, query)
  if (response.status === 401 && !path.startsWith('/auth/') && (await refreshSession())) {
    response = await send(method, path, body, query)
  }
  if (!response.ok) throw await toError(response)
  if (response.status === 204) return undefined as T
  return (await response.json()) as T
}

export const apiGet = <T>(path: string, query?: QueryParams) => request<T>('GET', path, undefined, query)
export const apiPost = <T>(path: string, body?: unknown) => request<T>('POST', path, body)
export const apiPut = <T>(path: string, body?: unknown) => request<T>('PUT', path, body ?? {})
export const apiPatch = <T>(path: string, body?: unknown) => request<T>('PATCH', path, body ?? {})
export const apiDelete = <T = void>(path: string) => request<T>('DELETE', path)
