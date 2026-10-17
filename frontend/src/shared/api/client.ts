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

export async function apiGet<T>(path: string): Promise<T> {
  const response = await fetch(toUrl(path), {
    headers: { Accept: 'application/json' },
    credentials: 'include',
  })
  if (!response.ok) {
    const contentType = response.headers.get('Content-Type') ?? ''
    if (contentType.includes('application/problem+json')) {
      const problem = (await response.json()) as ProblemDetails
      throw new ApiError(response.status, problem.title ?? response.statusText, problem.detail)
    }
    throw new ApiError(response.status, response.statusText)
  }
  return (await response.json()) as T
}
