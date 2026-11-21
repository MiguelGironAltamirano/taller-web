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

  it('marks all notifications as read when clicking Marcar todo como leído', async () => {
    let markAllCalled = false
    server.use(
      http.post('*/api/v1/notifications/read-all', () => {
        markAllCalled = true
        return new HttpResponse(null, { status: 204 })
      }),
    )

    const user = userEvent.setup()
    renderComponent()

    const markAllBtn = await screen.findByRole('button', { name: 'Marcar todo como leído' })
    await user.click(markAllBtn)

    expect(markAllCalled).toBe(true)
  })

  it('shows at most five, the unread ones first, and the total in «Ver todas»', async () => {
    server.use(
      http.get('*/api/v1/notifications', () =>
        HttpResponse.json(
          Array.from({ length: 7 }, (_, i) => ({
            id: i + 1,
            text: `Aviso número ${i + 1}.`,
            createdAt: `2026-10-17T0${i}:00:00Z`,
            read: i < 4, // the four oldest are read; the three newest are unread
            requestCode: null,
          })),
        ),
      ),
    )
    renderComponent()

    expect(await screen.findByText('Aviso número 7.')).toBeInTheDocument()
    const shown = screen.getAllByRole('button', { name: /^Aviso número \d\./ }).map((el) => /^Aviso número \d\./.exec(el.textContent ?? '')?.[0])
    expect(shown).toEqual(['Aviso número 7.', 'Aviso número 6.', 'Aviso número 5.', 'Aviso número 4.', 'Aviso número 3.'])
    expect(screen.getByRole('button', { name: 'Ver todas (7)' })).toBeInTheDocument()
  })

  it('goes to the full page from «Ver todas» and closes', async () => {
    const user = userEvent.setup()
    const { onClose } = renderComponent()
    await user.click(await screen.findByRole('button', { name: /^Ver todas \(2\)$/ }))
    expect(onClose).toHaveBeenCalled()
  })

  it('says there is nothing new when there are no notifications', async () => {
    server.use(http.get('*/api/v1/notifications', () => HttpResponse.json([])))
    renderComponent()
    expect(await screen.findByText('No tienes notificaciones nuevas')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Marcar todo como leído' })).toBeDisabled()
  })

  it('shows a loading state while the notifications load', () => {
    server.use(http.get('*/api/v1/notifications', () => new Promise(() => {})))
    renderComponent()
    expect(screen.getByRole('status', { name: 'Cargando' })).toBeInTheDocument()
  })

  it('dismisses when tapping outside', async () => {
    const user = userEvent.setup()
    const onDismiss = vi.fn()
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
    const { container } = render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <NotificationsPanel onClose={vi.fn()} onDismiss={onDismiss} />
        </MemoryRouter>
      </QueryClientProvider>,
    )
    await user.click(container.querySelector('[aria-hidden="true"].fixed') as HTMLElement)
    expect(onDismiss).toHaveBeenCalled()
  })
})
