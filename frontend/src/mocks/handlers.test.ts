import { beforeEach, describe, expect, it } from 'vitest'
import type { Page, RequestDetail, RequestSummary, TokenResponse } from '../shared/api/types'
import { server } from '../test/msw-server'
import { handlers, resetMockData } from './handlers'

const base = 'http://localhost:3000/api/v1'

async function login(email: string): Promise<string> {
  const response = await fetch(`${base}/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, password: 'Demo12345!' }),
  })
  const body = (await response.json()) as TokenResponse
  return body.accessToken
}

function auth(token: string) {
  return { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' }
}

describe('mock API', () => {
  beforeEach(() => {
    resetMockData()
    server.use(...handlers)
  })

  it('rejects requests without a token', async () => {
    const response = await fetch(`${base}/requests`)
    expect(response.status).toBe(401)
    expect(response.headers.get('Content-Type')).toContain('application/problem+json')
  })

  it('creates a request with a SOL code and lists it for its requester', async () => {
    const token = await login('estudiante@unmsm.edu.pe')
    const created = await fetch(`${base}/requests`, {
      method: 'POST',
      headers: auth(token),
      body: JSON.stringify({ categoryId: 1, locationId: 3, description: 'El tomacorriente del aula no funciona', priorityId: 2 }),
    })
    const detail = (await created.json()) as RequestDetail
    expect(created.status).toBe(201)
    expect(detail.code).toMatch(/^SOL-\d{4}-\d{6}$/)
    expect(detail.status).toBe('REGISTRADA')

    const list = (await (await fetch(`${base}/requests`, { headers: auth(token) })).json()) as Page<RequestSummary>
    expect(list.items.map((item) => item.code)).toContain(detail.code)
  })

  it('rejects an invalid transition with 409', async () => {
    const student = await login('estudiante@unmsm.edu.pe')
    const created = (await (
      await fetch(`${base}/requests`, {
        method: 'POST',
        headers: auth(student),
        body: JSON.stringify({ categoryId: 1, locationId: 3, description: 'La luz del pasillo parpadea siempre', priorityId: 3 }),
      })
    ).json()) as RequestDetail
    const supervisor = await login('supervisor.mantenimiento@unmsm.edu.pe')
    const response = await fetch(`${base}/requests/${created.code}/transitions`, {
      method: 'POST',
      headers: auth(supervisor),
      body: JSON.stringify({ to: 'RESUELTA' }),
    })
    expect(response.status).toBe(409)
  })
})

describe('mock API registration', () => {
  beforeEach(() => {
    resetMockData()
    server.use(...handlers)
  })

  const register = (email: string) =>
    fetch(`${base}/auth/register`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ fullName: 'Ana Quispe', email, password: 'Clave12345', requesterType: 'DOCENTE' }),
    })

  it('creates a new UNMSM account with 201', async () => {
    expect((await register('nueva@unmsm.edu.pe')).status).toBe(201)
  })

  it('answers 409 for a seed email', async () => {
    expect((await register('estudiante@unmsm.edu.pe')).status).toBe(409)
  })
})
