import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { delay, http, HttpResponse } from 'msw'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { getDb } from '../../mocks/db'
import { handlers, resetMockData } from '../../mocks/handlers'
import { tokenFor } from '../../mocks/http'
import { sessionStore } from '../../shared/session/session-store'
import { ToastProvider } from '../../shared/ui'
import { server } from '../../test/msw-server'
import { ActionModal } from './ActionModal'

beforeEach(() => {
  resetMockData()
  server.use(...handlers)
  const lucia = getDb().users.find((u) => u.id === 1)!
  sessionStore.set({
    accessToken: tokenFor(lucia),
    user: { id: 1, fullName: lucia.fullName, email: lucia.email, role: 'SOLICITANTE', areaId: null },
  })
})

describe('ActionModal', () => {
  it('keeps the reason typed before the catalogs finished loading', async () => {
    let release: () => void = () => {}
    const catalogsArrive = new Promise<void>((resolve) => (release = resolve))
    server.use(
      http.get('*/api/v1/catalogs', async () => {
        await catalogsArrive
        await delay(0)
        const { areas, categories, priorities, locations } = getDb()
        return HttpResponse.json({ areas, categories, priorities, locations })
      }),
    )
    const user = userEvent.setup()
    render(
      <QueryClientProvider client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}>
        <ToastProvider>
          <ActionModal
            action="cancel"
            target={{ code: 'SOL-2026-000001', area: 'Mantenimiento', priority: 'Media', technician: null, createdAt: Date.now() }}
            onClose={() => {}}
          />
        </ToastProvider>
      </QueryClientProvider>,
    )

    await user.type(screen.getByLabelText('¿Por qué la cancelas?'), 'Ya lo arreglaron')
    release()

    await waitFor(() => expect(screen.getByLabelText('¿Por qué la cancelas?')).toHaveValue('Ya lo arreglaron'))
    // Once more, after the catalogs certainly arrived and the form had a chance to update.
    await new Promise((resolve) => setTimeout(resolve, 50))
    expect(screen.getByLabelText('¿Por qué la cancelas?')).toHaveValue('Ya lo arreglaron')
  })

  // HU-19, escenario «En la interfaz»: un 409 del servidor (la transición ya no está permitida porque la solicitud
  // cambió) se explica en lenguaje claro, se cierra el diálogo y se refresca el detalle con su estado actual.
  it('explains a 409, closes the dialog and refreshes the request', async () => {
    server.use(
      http.post('*/api/v1/requests/:code/transitions', () =>
        HttpResponse.json(
          { type: 'about:blank', title: 'Conflict', status: 409, detail: 'La transición no está permitida' },
          { status: 409, headers: { 'Content-Type': 'application/problem+json' } },
        ),
      ),
    )
    const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
    const invalidate = vi.spyOn(client, 'invalidateQueries')
    const onClose = vi.fn()
    const user = userEvent.setup()
    render(
      <QueryClientProvider client={client}>
        <ToastProvider>
          <ActionModal
            action="cancel"
            target={{ code: 'SOL-2026-000001', area: 'Mantenimiento', priority: 'Media', technician: null, createdAt: Date.now() }}
            onClose={onClose}
          />
        </ToastProvider>
      </QueryClientProvider>,
    )

    await user.type(await screen.findByLabelText('¿Por qué la cancelas?'), 'Ya lo arreglaron')
    await user.click(screen.getByRole('button', { name: 'Cancelar solicitud' }))

    expect(await screen.findByText('La solicitud cambió')).toBeInTheDocument()
    expect(screen.getByText(/Otra persona actualizó esta solicitud/)).toBeInTheDocument()
    expect(invalidate).toHaveBeenCalledWith({ queryKey: ['requests'] })
    expect(onClose).toHaveBeenCalled()
  })
})
