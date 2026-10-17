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

// El token de acceso vive solo en memoria (spec §3.3); nunca en localStorage.
let accessToken: string | null = null
let onUnauthorized: (() => void) | null = null

export const setAccessToken = (token: string | null) => {
  accessToken = token
}
export const getAccessToken = () => accessToken
/** Registra qué hacer cuando una petición autenticada devuelve 401 (sesión vencida). */
export const setUnauthorizedHandler = (handler: (() => void) | null) => {
  onUnauthorized = handler
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

async function request<T>(method: Method, path: string, body?: unknown, query?: QueryParams): Promise<T> {
  const headers: Record<string, string> = { Accept: 'application/json' }
  if (body !== undefined) headers['Content-Type'] = 'application/json'
  if (accessToken) headers.Authorization = `Bearer ${accessToken}`

  const response = await fetch(toUrl(path, query), {
    method,
    headers,
    credentials: 'include',
    body: body === undefined ? undefined : JSON.stringify(body),
  })

  if (!response.ok) {
    if (response.status === 401 && accessToken) onUnauthorized?.()
    const contentType = response.headers.get('Content-Type') ?? ''
    if (contentType.includes('application/problem+json')) {
      const problem = (await response.json()) as ProblemDetails
      throw new ApiError(response.status, problem.title ?? response.statusText, problem.detail, problem.errors)
    }
    throw new ApiError(response.status, response.statusText)
  }
  if (response.status === 204) return undefined as T
  return (await response.json()) as T
}

export const apiGet = <T>(path: string, query?: QueryParams) => request<T>('GET', path, undefined, query)
export const apiPost = <T>(path: string, body?: unknown) => request<T>('POST', path, body ?? {})
export const apiPut = <T>(path: string, body?: unknown) => request<T>('PUT', path, body ?? {})
export const apiPatch = <T>(path: string, body?: unknown) => request<T>('PATCH', path, body ?? {})
export const apiDelete = <T = void>(path: string) => request<T>('DELETE', path)
