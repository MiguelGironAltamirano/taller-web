import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import { server } from '../../test/msw-server'
import { sessionStore } from '../session/session-store'
import { ApiError, apiGet, apiPost } from './client'

describe('apiGet', () => {
  it('returns the parsed JSON body on success', async () => {
    server.use(http.get('*/api/v1/things', () => HttpResponse.json({ id: 1 })))
    await expect(apiGet<{ id: number }>('/things')).resolves.toEqual({ id: 1 })
  })

  it('throws ApiError with problem details fields', async () => {
    server.use(
      http.get('*/api/v1/things', () =>
        HttpResponse.json(
          { title: 'Not Found', status: 404, detail: 'No existe' },
          { status: 404, headers: { 'Content-Type': 'application/problem+json' } },
        ),
      ),
    )
    const error = await apiGet('/things').catch((e: unknown) => e)
    expect(error).toBeInstanceOf(ApiError)
    expect(error).toMatchObject({ status: 404, title: 'Not Found', detail: 'No existe' })
  })

  it('throws ApiError with the status text when the body is not problem details', async () => {
    server.use(http.get('*/api/v1/things', () => new HttpResponse('boom', { status: 503, statusText: 'Service Unavailable' })))
    await expect(apiGet('/things')).rejects.toMatchObject({ status: 503, title: 'Service Unavailable' })
  })
})

describe('session handling', () => {
  const user = { id: 4, fullName: 'Estudiante Demo', email: 'estudiante@unmsm.edu.pe', role: 'SOLICITANTE' as const, areaId: null }

  it('sends the bearer token and a JSON body with apiPost', async () => {
    sessionStore.set({ accessToken: 'abc', user })
    let seen: { auth: string | null; body: unknown } | undefined
    server.use(
      http.post('*/api/v1/things', async ({ request }) => {
        seen = { auth: request.headers.get('Authorization'), body: await request.json() }
        return HttpResponse.json({ ok: true })
      }),
    )
    await expect(apiPost('/things', { a: 1 })).resolves.toEqual({ ok: true })
    expect(seen).toEqual({ auth: 'Bearer abc', body: { a: 1 } })
  })

  it('refreshes once on a 401 and retries the original request with the new token', async () => {
    sessionStore.set({ accessToken: 'old', user })
    let refreshCalls = 0
    server.use(
      http.post('*/api/v1/auth/refresh', () => {
        refreshCalls += 1
        return HttpResponse.json({ accessToken: 'new', expiresIn: 900, user })
      }),
      http.get('*/api/v1/things', ({ request }) =>
        request.headers.get('Authorization') === 'Bearer new'
          ? HttpResponse.json({ id: 1 })
          : new HttpResponse(null, { status: 401 }),
      ),
    )
    await expect(apiGet('/things')).resolves.toEqual({ id: 1 })
    expect(refreshCalls).toBe(1)
  })

  it('clears the session when the refresh fails', async () => {
    sessionStore.set({ accessToken: 'old', user })
    server.use(
      http.post('*/api/v1/auth/refresh', () => new HttpResponse(null, { status: 401 })),
      http.get('*/api/v1/things', () => new HttpResponse(null, { status: 401 })),
    )
    await expect(apiGet('/things')).rejects.toMatchObject({ status: 401 })
    expect(sessionStore.get()).toBeNull()
  })

  it('does not try to refresh when an /auth call returns 401', async () => {
    let refreshCalls = 0
    server.use(
      http.post('*/api/v1/auth/refresh', () => {
        refreshCalls += 1
        return new HttpResponse(null, { status: 401 })
      }),
      http.post('*/api/v1/auth/login', () => new HttpResponse(null, { status: 401 })),
    )
    await expect(apiPost('/auth/login', {})).rejects.toMatchObject({ status: 401 })
    expect(refreshCalls).toBe(0)
  })
})
