import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import type { RequestDetailView } from '../../shared/domain'
import { ToastProvider } from '../../shared/ui'
import { server } from '../../test/msw-server'
import { SatisfactionPanel } from './SatisfactionPanel'

const OWNER = { id: 10, role: 'solicitante' as const }
const OTHER_REQUESTER = { id: 11, role: 'solicitante' as const }
const SUPERVISOR = { id: 2, role: 'supervisor' as const }

function detail(overrides: Partial<RequestDetailView> = {}): RequestDetailView {
  return {
    code: 'SOL-2026-000001',
    createdAt: Date.parse('2026-11-10T10:00:00Z'),
    dueAt: Date.parse('2026-11-10T18:00:00Z'),
    state: 'Resuelta',
    category: 'Electricidad',
    priority: 'Alta',
    faculty: 'Sistemas',
    pavilion: 'Pabellón Antiguo',
    room: 'Aula 101',
    description: 'El tomacorriente del aula no funciona',
    requester: 'Ana Solicitante',
    area: 'Mantenimiento',
    technician: 'Técnico Demo',
    areaId: 1,
    requesterId: OWNER.id,
    technicianId: 3,
    history: [
      { from: '—', to: 'Registrada', reason: '', who: 'Ana Solicitante', at: 1 },
      { from: 'En atención', to: 'Resuelta', reason: 'Solución: Se cambió el tomacorriente', who: 'Técnico Demo', at: 2 },
    ],
    comments: [],
    evidences: [],
    solution: 'Se cambió el tomacorriente',
    rating: null,
    autoCloseAt: null,
    allowedActions: [],
    closureReason: null,
    ...overrides,
  }
}

function renderPanel(request: RequestDetailView, user: { id: number; role: 'solicitante' | 'supervisor' } = OWNER) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } })
  return render(
    <QueryClientProvider client={client}>
      <ToastProvider>
        <SatisfactionPanel request={request} user={user} />
      </ToastProvider>
    </QueryClientProvider>,
  )
}

const problem = (status: number, detail: string) =>
  HttpResponse.json({ type: 'about:blank', title: 'Conflict', status, detail }, {
    status,
    headers: { 'Content-Type': 'application/problem+json' },
  })

/** Registra lo que la tarjeta envía al backend, en orden. */
function recordCalls(responses: { transitions?: () => Response; rating?: () => Response } = {}) {
  const calls: { path: string; body: unknown }[] = []
  server.use(
    http.post('*/api/v1/requests/:code/transitions', async ({ request }) => {
      calls.push({ path: 'transitions', body: await request.json() })
      return responses.transitions?.() ?? HttpResponse.json({})
    }),
    http.post('*/api/v1/requests/:code/rating', async ({ request }) => {
      calls.push({ path: 'rating', body: await request.json() })
      return responses.rating?.() ?? HttpResponse.json({})
    }),
  )
  return calls
}

describe('SatisfactionPanel: solicitud resuelta', () => {
  it('muestra la solución propuesta al solicitante dueño', () => {
    renderPanel(detail())
    expect(screen.getByRole('heading', { name: '¿Quedó resuelto?' })).toBeInTheDocument()
    expect(screen.getByText('Se cambió el tomacorriente')).toBeInTheDocument()
  })

  it('sin solución registrada no muestra un recuadro vacío', () => {
    renderPanel(detail({ solution: null }))
    expect(screen.getByRole('heading', { name: '¿Quedó resuelto?' })).toBeInTheDocument()
    expect(screen.queryByText(/Solución propuesta/)).not.toBeInTheDocument()
  })

  it('no se muestra a otro solicitante ni al personal', () => {
    const { unmount } = renderPanel(detail(), OTHER_REQUESTER)
    expect(screen.queryByRole('heading', { name: '¿Quedó resuelto?' })).not.toBeInTheDocument()
    unmount()
    renderPanel(detail(), SUPERVISOR)
    expect(screen.queryByRole('heading', { name: '¿Quedó resuelto?' })).not.toBeInTheDocument()
  })

  it('confirmar sin calificar solo cierra la solicitud', async () => {
    const calls = recordCalls()
    renderPanel(detail())

    await userEvent.click(screen.getByRole('button', { name: 'Confirmar y cerrar' }))

    expect(await screen.findByText('Solicitud cerrada.')).toBeInTheDocument()
    expect(calls).toEqual([{ path: 'transitions', body: { to: 'CERRADA' } }])
  })

  it('confirmar con estrellas cierra y después guarda la calificación', async () => {
    const calls = recordCalls()
    renderPanel(detail())

    await userEvent.click(screen.getByRole('radio', { name: '4 estrellas' }))
    await userEvent.type(screen.getByRole('textbox', { name: 'Comentario' }), '  Muy rápido  ')
    await userEvent.click(screen.getByRole('button', { name: 'Confirmar y cerrar' }))

    expect(await screen.findByText('Solicitud cerrada. Gracias por tu calificación.')).toBeInTheDocument()
    expect(calls).toEqual([
      { path: 'transitions', body: { to: 'CERRADA' } },
      { path: 'rating', body: { score: 4, comment: 'Muy rápido' } },
    ])
  })

  it('si la calificación falla, avisa que la solicitud sí se cerró', async () => {
    recordCalls({ rating: () => problem(500, 'Error') })
    renderPanel(detail())

    await userEvent.click(screen.getByRole('radio', { name: '5 estrellas' }))
    await userEvent.click(screen.getByRole('button', { name: 'Confirmar y cerrar' }))

    expect(
      await screen.findByText('Solicitud cerrada, pero no pudimos guardar tu calificación. Puedes volver a enviarla.'),
    ).toBeInTheDocument()
  })

  it('si el cierre falla, muestra el motivo del servidor y no califica', async () => {
    const calls = recordCalls({ transitions: () => problem(409, 'Transición no permitida: de CERRADA a CERRADA') })
    renderPanel(detail())

    await userEvent.click(screen.getByRole('radio', { name: '3 estrellas' }))
    await userEvent.click(screen.getByRole('button', { name: 'Confirmar y cerrar' }))

    expect(await screen.findByText('Transición no permitida: de CERRADA a CERRADA')).toBeInTheDocument()
    expect(calls.map((c) => c.path)).toEqual(['transitions'])
  })

  it('rechazar exige un motivo de al menos 10 caracteres', async () => {
    const calls = recordCalls()
    renderPanel(detail())

    await userEvent.click(screen.getByRole('button', { name: 'Rechazar solución' }))
    const dialog = await screen.findByRole('dialog', { name: 'Rechazar solución' })
    await userEvent.type(screen.getByRole('textbox', { name: 'Motivo' }), 'Corto')
    await userEvent.click(screen.getAllByRole('button', { name: 'Rechazar solución' }).at(-1)!)

    expect(dialog).toHaveTextContent('Indica un motivo de al menos 10 caracteres.')
    expect(calls).toEqual([])
  })

  it('rechazar con motivo devuelve la solicitud a En atención', async () => {
    const calls = recordCalls()
    renderPanel(detail())

    await userEvent.click(screen.getByRole('button', { name: 'Rechazar solución' }))
    await userEvent.type(await screen.findByRole('textbox', { name: 'Motivo' }), 'El tomacorriente sigue sin funcionar')
    await userEvent.click(screen.getAllByRole('button', { name: 'Rechazar solución' }).at(-1)!)

    expect(await screen.findByText('Solución rechazada. La solicitud volvió a En atención.')).toBeInTheDocument()
    expect(calls).toEqual([
      { path: 'transitions', body: { to: 'EN_ATENCION', reason: 'El tomacorriente sigue sin funcionar' } },
    ])
  })
})

describe('SatisfactionPanel: solicitud cerrada', () => {
  const confirmed = (rating: RequestDetailView['rating']) =>
    detail({ state: 'Cerrada', closureReason: 'CONFIRMADA', rating })

  it('muestra al solicitante su calificación, sin formulario', () => {
    renderPanel(confirmed({ score: 4, comment: 'Muy rápido' }))

    expect(screen.getByRole('img', { name: 'Tu calificación: 4 de 5 estrellas' })).toBeInTheDocument()
    expect(screen.getByText('“Muy rápido”')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Enviar calificación' })).not.toBeInTheDocument()
  })

  it('el personal ve la calificación del solicitante', () => {
    renderPanel(confirmed({ score: 2, comment: '' }), SUPERVISOR)

    expect(screen.getByRole('heading', { name: 'Calificación del solicitante' })).toBeInTheDocument()
    expect(screen.getByRole('img', { name: 'Calificación: 2 de 5 estrellas' })).toBeInTheDocument()
  })

  it('sin calificación, el solicitante puede calificar después', async () => {
    const calls = recordCalls()
    renderPanel(confirmed(null))

    const send = screen.getByRole('button', { name: 'Enviar calificación' })
    expect(send).toBeDisabled()
    await userEvent.click(screen.getByRole('radio', { name: '5 estrellas' }))
    await userEvent.click(send)

    expect(await screen.findByText('Gracias por tu calificación.')).toBeInTheDocument()
    expect(calls).toEqual([{ path: 'rating', body: { score: 5 } }])
  })

  it('si ya estaba calificada, muestra el motivo del servidor', async () => {
    recordCalls({ rating: () => problem(409, 'Ya calificaste esta solicitud') })
    renderPanel(confirmed(null))

    await userEvent.click(screen.getByRole('radio', { name: '3 estrellas' }))
    await userEvent.click(screen.getByRole('button', { name: 'Enviar calificación' }))

    expect(await screen.findByText('Ya calificaste esta solicitud')).toBeInTheDocument()
  })

  it('el personal no puede calificar en nombre del solicitante', () => {
    renderPanel(confirmed(null), SUPERVISOR)
    expect(screen.queryByRole('button', { name: 'Enviar calificación' })).not.toBeInTheDocument()
  })

  it('explica el cierre automático sin fijar un número de horas (el plazo es configurable)', () => {
    renderPanel(detail({ state: 'Cerrada', closureReason: 'AUTOMATICA' }))

    expect(screen.getByRole('heading', { name: 'Cerrada automáticamente' })).toBeInTheDocument()
    expect(screen.getByText(/no confirmaste ni rechazaste la solución dentro del plazo/)).toBeInTheDocument()
    expect(screen.queryByText(/72 horas/)).not.toBeInTheDocument()
    expect(screen.queryByRole('radiogroup')).not.toBeInTheDocument()
  })

  it('no muestra nada en un cierre por no procede', () => {
    renderPanel(detail({ state: 'Cerrada', closureReason: 'NO_PROCEDE' }))
    expect(screen.queryByRole('heading')).not.toBeInTheDocument()
  })
})
