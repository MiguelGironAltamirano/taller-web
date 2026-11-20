import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { beforeEach, describe, expect, it } from 'vitest'
import { handlers, resetMockData } from '../../mocks/handlers'
import { server } from '../../test/msw-server'
import { renderApp } from '../../test/render-app'

const REPLACED = 'Tu sesión se cerró porque ingresaste desde otro dispositivo'

/** Respuesta del backend cuando el mismo usuario ingresó en otro dispositivo (contrato «Una sola sesión por usuario»). */
const replaced = () =>
  HttpResponse.json(
    { title: 'Unauthorized', status: 401, detail: REPLACED, code: 'SESSION_REPLACED' },
    { status: 401, headers: { 'Content-Type': 'application/problem+json' } },
  )

beforeEach(() => {
  resetMockData()
  server.use(...handlers)
})

describe('Sesión cerrada desde otro dispositivo', () => {
  it('explains on the login page why the session ended', async () => {
    server.use(http.post('*/api/v1/auth/refresh', replaced))
    renderApp('/inicio')

    expect(await screen.findByRole('heading', { name: 'Iniciar sesión' })).toBeInTheDocument()
    expect(screen.getByRole('status')).toHaveTextContent(REPLACED)
  })

  it('hides the notice once the user logs in again', async () => {
    server.use(http.post('*/api/v1/auth/refresh', replaced))
    const user = userEvent.setup()
    renderApp('/inicio')
    await screen.findByText(REPLACED, { exact: false })

    await user.type(screen.getByLabelText('Correo'), 'lquispe@unmsm.edu.pe')
    await user.type(screen.getByLabelText('Contraseña'), 'Demo12345!')
    await user.click(screen.getByRole('button', { name: /^Ingresar$/ }))

    expect(await screen.findByRole('heading', { name: 'Hola, Lucía' })).toBeInTheDocument()
    expect(screen.queryByText(REPLACED, { exact: false })).not.toBeInTheDocument()
  })

  it('notices it when the window gets the focus back, even if nothing else asks the server', async () => {
    const lucia = { id: 1, fullName: 'Lucía Quispe Huamán', email: 'lquispe@unmsm.edu.pe', role: 'SOLICITANTE', areaId: null }
    // Open session when the app loads; from then on, another device has taken it over.
    server.use(
      http.post('*/api/v1/auth/refresh', replaced),
      http.get('*/api/v1/me', replaced),
    )
    server.use(
      http.post('*/api/v1/auth/refresh', () => HttpResponse.json({ accessToken: 'mock-token-1', expiresIn: 900, user: lucia }), {
        once: true,
      }),
    )
    renderApp('/inicio')
    expect(await screen.findByRole('heading', { name: 'Hola, Lucía' })).toBeInTheDocument()

    window.dispatchEvent(new Event('focus'))

    expect(await screen.findByRole('heading', { name: 'Iniciar sesión' })).toBeInTheDocument()
    expect(screen.getByRole('status')).toHaveTextContent(REPLACED)
  })

  it('says nothing when the session simply expired', async () => {
    server.use(http.post('*/api/v1/auth/refresh', () => new HttpResponse(null, { status: 401 })))
    renderApp('/inicio')

    expect(await screen.findByRole('heading', { name: 'Iniciar sesión' })).toBeInTheDocument()
    expect(screen.queryByText(REPLACED, { exact: false })).not.toBeInTheDocument()
  })
})
