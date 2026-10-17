import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { MemoryRouter } from 'react-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { server } from '../../test/msw-server'
import { NotificationsPanel } from './NotificationsPanel'

function renderComponent(onClose = vi.fn()) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return {
    ...render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <NotificationsPanel onClose={onClose} />
        </MemoryRouter>
      </QueryClientProvider>,
    ),
    onClose,
  }
}

describe('NotificationsPanel', () => {
  beforeEach(() => {
    server.use(
      http.get('*/api/v1/notifications', () => {
        return HttpResponse.json([
          {
            id: 1,
            text: 'La solicitud SOL-2026-000001 pasó a En atención.',
            createdAt: '2026-10-17T12:00:00Z',
            read: false,
            requestCode: 'SOL-2026-000001',
          },
          {
            id: 2,
            text: 'Se te asignó la solicitud SOL-2026-000002.',
            createdAt: '2026-10-17T11:00:00Z',
            read: true,
            requestCode: 'SOL-2026-000002',
          },
        ])
      }),
    )
  })

  it('renders notifications list', async () => {
    renderComponent()

    expect(await screen.findByText('La solicitud SOL-2026-000001 pasó a En atención.')).toBeInTheDocument()
    expect(screen.getByText('Se te asignó la solicitud SOL-2026-000002.')).toBeInTheDocument()
  })

  it('marks single notification as read when clicked', async () => {
    let patchedId: string | null = null
    server.use(
      http.patch('*/api/v1/notifications/:id/read', ({ params }) => {
        patchedId = String(params.id)
        return new HttpResponse(null, { status: 204 })
      }),
    )

    const user = userEvent.setup()
    const { onClose } = renderComponent()

    const unreadItem = await screen.findByText('La solicitud SOL-2026-000001 pasó a En atención.')
    await user.click(unreadItem)

    expect(patchedId).toBe('1')
    expect(onClose).toHaveBeenCalled()
  })

  it('marks all notifications as read when clicking Marcar todas como leídas', async () => {
    let markAllCalled = false
    server.use(
      http.post('*/api/v1/notifications/read-all', () => {
        markAllCalled = true
        return new HttpResponse(null, { status: 204 })
      }),
    )

    const user = userEvent.setup()
    renderComponent()

    const markAllBtn = await screen.findByRole('button', { name: 'Marcar todas como leídas' })
    await user.click(markAllBtn)

    expect(markAllCalled).toBe(true)
  })
})
