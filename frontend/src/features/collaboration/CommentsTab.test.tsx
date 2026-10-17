import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { beforeEach, describe, expect, it } from 'vitest'
import type { RequestDetailView } from '../../shared/domain'
import { ToastProvider } from '../../shared/ui'
import { server } from '../../test/msw-server'
import { CommentsTab } from './CommentsTab'

const mockRequest: RequestDetailView = {
  code: 'SOL-2026-000001',
  createdAt: Date.parse('2026-10-17T10:00:00Z'),
  dueAt: Date.parse('2026-10-17T18:00:00Z'),
  state: 'Registrada',
  category: 'Electricidad',
  priority: 'Alta',
  faculty: 'Sistemas',
  pavilion: 'Pabellón Antiguo',
  room: 'Aula 101',
  description: 'Problema eléctrico',
  requester: 'Ana Solicitante',
  area: 'Mantenimiento',
  technician: null,
  areaId: 1,
  requesterId: 10,
  technicianId: null,
  history: [],
  comments: [],
  evidences: [],
  solution: null,
  rating: null,
  allowedActions: [],
}

function renderComponent(req = mockRequest, userName = 'Ana Solicitante') {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={queryClient}>
      <ToastProvider>
        <CommentsTab request={req} currentUserName={userName} />
      </ToastProvider>
    </QueryClientProvider>,
  )
}

describe('CommentsTab', () => {
  beforeEach(() => {
    server.use(
      http.get('*/api/v1/requests/:code/comments', () => {
        return HttpResponse.json([
          {
            id: 1,
            authorName: 'Ana Solicitante',
            text: 'Comentario existente',
            createdAt: '2026-10-17T11:00:00Z',
          },
        ])
      }),
    )
  })

  it('renders existing comments from query', async () => {
    renderComponent()
    expect(await screen.findByText('Comentario existente')).toBeInTheDocument()
    expect(screen.getByText('Ana Solicitante')).toBeInTheDocument()
  })

  it('submits a new comment and resets input on success', async () => {
    let submittedText = ''
    server.use(
      http.post('*/api/v1/requests/:code/comments', async ({ request }) => {
        const body = (await request.json()) as { text: string }
        submittedText = body.text
        return HttpResponse.json(
          {
            id: 2,
            authorName: 'Ana Solicitante',
            text: body.text,
            createdAt: '2026-10-17T12:00:00Z',
          },
          { status: 201 },
        )
      }),
    )

    const user = userEvent.setup()
    renderComponent()

    const input = await screen.findByRole('textbox', { name: 'Escribe un comentario' })
    await user.type(input, 'Nuevo comentario de prueba')
    await user.click(screen.getByRole('button', { name: 'Enviar' }))

    expect(submittedText).toBe('Nuevo comentario de prueba')
    expect(input).toHaveValue('')
  })

  it('shows toast when server rejects the comment', async () => {
    server.use(
      http.post('*/api/v1/requests/:code/comments', () => {
        return HttpResponse.json(
          {
            type: 'about:blank',
            title: 'Bad Request',
            status: 400,
            detail: 'El comentario no puede superar los 1000 caracteres',
          },
          { status: 400, headers: { 'Content-Type': 'application/problem+json' } },
        )
      }),
    )

    const user = userEvent.setup()
    renderComponent()

    const input = await screen.findByRole('textbox', { name: 'Escribe un comentario' })
    await user.type(input, 'Texto rechazado')
    await user.click(screen.getByRole('button', { name: 'Enviar' }))

    expect(await screen.findByText('El comentario no puede superar los 1000 caracteres')).toBeInTheDocument()
  })
})
