import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import { server } from '../../test/msw-server'
import { ApiError, apiGet } from './client'

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
