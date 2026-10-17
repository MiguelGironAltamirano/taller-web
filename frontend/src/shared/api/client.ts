import { sessionStore } from '../session/session-store'
import type { TokenResponse } from './types'

const API_BASE = import.meta.env.VITE_API_BASE ?? '/api/v1'

type ProblemDetails = { title?: string; status?: number; detail?: string }

export class ApiError extends Error {
  readonly status: number
  readonly title: string
  readonly detail?: string

  constructor(status: number, title: string, detail?: string) {
    super(detail ?? title)
    this.name = 'ApiError'
    this.status = status
    this.title = title
    this.detail = detail
  }
}

function toUrl(path: string): string {
  return new URL(`${API_BASE}${path}`, window.location.origin).toString()
}

function send(method: 'GET' | 'POST', path: string, body?: unknown): Promise<Response> {
  const headers: Record<string, string> = { Accept: 'application/json' }
  const session = sessionStore.get()
  if (session) headers.Authorization = `Bearer ${session.accessToken}`
  if (body !== undefined) headers['Content-Type'] = 'application/json'
  return fetch(toUrl(path), {
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
    return new ApiError(response.status, problem.title ?? response.statusText, problem.detail)
  }
  return new ApiError(response.status, response.statusText)
}

async function request<T>(method: 'GET' | 'POST', path: string, body?: unknown): Promise<T> {
  let response = await send(method, path, body)
  if (response.status === 401 && !path.startsWith('/auth/') && (await refreshSession())) {
    response = await send(method, path, body)
  }
  if (!response.ok) throw await toError(response)
  if (response.status === 204) return undefined as T
  return (await response.json()) as T
}

export function apiGet<T>(path: string): Promise<T> {
  return request<T>('GET', path)
}

export function apiPost<T>(path: string, body?: unknown): Promise<T> {
  return request<T>('POST', path, body)
}
