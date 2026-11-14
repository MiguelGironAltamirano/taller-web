import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import type { ReactElement } from 'react'
import type { Catalogs, Technician } from '../../shared/api/types'
import type { RequestDetailView, RequestRow } from '../../shared/domain'
import { ToastProvider } from '../../shared/ui'
import { server } from '../../test/msw-server'

export const CATALOGS: Catalogs = {
  areas: [
    { id: 1, name: 'Mantenimiento' },
    { id: 2, name: 'Soporte TI' },
  ],
  categories: [{ id: 1, name: 'Electricidad', areaId: 1 }],
  priorities: [
    { id: 1, name: 'Crítica', slaHours: 4 },
    { id: 2, name: 'Alta', slaHours: 8 },
    { id: 3, name: 'Media', slaHours: 24 },
    { id: 4, name: 'Baja', slaHours: 72 },
  ],
  locations: [],
}

export const TECHNICIANS: Technician[] = [
  { id: 3, fullName: 'Tomás Técnico', activeRequests: 2 },
  { id: 4, fullName: 'Teresa Técnica', activeRequests: 0 },
]

export const problem = (status: number, title: string, extra: Record<string, unknown> = {}) =>
  HttpResponse.json({ type: 'about:blank', title, status, ...extra }, {
    status,
    headers: { 'Content-Type': 'application/problem+json' },
  })

export function row(overrides: Partial<RequestRow> = {}): RequestRow {
  return {
    code: 'SOL-2026-000001',
    requester: 'Ana Solicitante',
    category: 'Electricidad',
    area: 'Mantenimiento',
    faculty: 'Sistemas',
    pavilion: 'Pabellón Antiguo',
    room: 'Aula 101',
    description: 'El tomacorriente del aula no funciona',
    priority: 'Alta',
    state: 'Registrada',
    technician: null,
    createdAt: Date.parse('2026-11-10T10:00:00Z'),
    dueAt: Date.parse('2026-11-10T18:00:00Z'),
    ...overrides,
  }
}

export function detail(overrides: Partial<RequestDetailView> = {}): RequestDetailView {
  return {
    ...row(),
    areaId: 1,
    requesterId: 10,
    technicianId: null,
    history: [],
    comments: [],
    evidences: [],
    solution: null,
    rating: null,
    allowedActions: [],
    closureReason: null,
    ...overrides,
  }
}

/** Lo que la interfaz envía al backend, en orden. */
export type Call = { path: string; body: unknown }

/**
 * Simula el backend del flujo: catálogos, técnicos del área y las dos escrituras. Devuelve las llamadas recibidas
 * para comprobar el contrato (`/transitions` o `/priority`) y cuántas veces se pidió la lista de técnicos.
 */
export function mockWorkflowApi(
  responses: { transitions?: () => Response; priority?: () => Response; technicians?: Technician[] } = {},
) {
  const calls: Call[] = []
  const technicianAreas: (string | null)[] = []
  server.use(
    http.get('*/api/v1/catalogs', () => HttpResponse.json(CATALOGS)),
    http.get('*/api/v1/users/technicians', ({ request }) => {
      technicianAreas.push(new URL(request.url).searchParams.get('areaId'))
      return HttpResponse.json(responses.technicians ?? TECHNICIANS)
    }),
    http.post('*/api/v1/requests/:code/transitions', async ({ request, params }) => {
      calls.push({ path: `${params.code}/transitions`, body: await request.json() })
      return responses.transitions?.() ?? HttpResponse.json({})
    }),
    http.patch('*/api/v1/requests/:code/priority', async ({ request, params }) => {
      calls.push({ path: `${params.code}/priority`, body: await request.json() })
      return responses.priority?.() ?? HttpResponse.json({})
    }),
  )
  return { calls, technicianAreas }
}

/** Renderiza con los proveedores que necesita el flujo (consultas y avisos). Devuelve el cliente para inspeccionarlo. */
export function renderWithProviders(ui: ReactElement) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } })
  const result = render(
    <QueryClientProvider client={client}>
      <ToastProvider>{ui}</ToastProvider>
    </QueryClientProvider>,
  )
  return { ...result, client }
}
