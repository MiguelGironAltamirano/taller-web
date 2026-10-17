import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { beforeEach, describe, expect, it } from 'vitest'
import { handlers, resetMockData } from '../mocks/handlers'
import { server } from '../test/msw-server'
import { renderApp } from '../test/render-app'

const SOLICITANTE = 'lquispe@unmsm.edu.pe'
const SUPERVISOR = 'avillanueva@unmsm.edu.pe'

async function signIn(email: string, password = 'Demo12345!') {
  const user = userEvent.setup()
  await user.type(await screen.findByLabelText('Correo'), email)
  await user.type(screen.getByLabelText('Contraseña'), password)
  await user.click(screen.getByRole('button', { name: 'Ingresar' }))
  return user
}

/** Simula la cookie de renovación: el servidor devuelve la sesión de la solicitante. */
const refreshAsRequester = http.post('*/api/v1/auth/refresh', () =>
  HttpResponse.json({
    accessToken: 'mock-token-1',
    expiresIn: 900,
    user: { id: 1, fullName: 'Lucía Quispe Huamán', email: SOLICITANTE, role: 'SOLICITANTE', areaId: null },
  }),
)

describe('App', () => {
  beforeEach(() => {
    resetMockData()
    server.use(...handlers)
  })

  it('redirects to the login page when there is no session', async () => {
    renderApp('/')
    expect(await screen.findByRole('heading', { name: 'Iniciar sesión' })).toBeInTheDocument()
  })

  it('remembers the requested page and goes there after login', async () => {
    renderApp('/notificaciones')
    await signIn(SOLICITANTE)
    expect(await screen.findByRole('heading', { name: 'Notificaciones' })).toBeInTheDocument()
  })

  it('sends a requester to the home page after login', async () => {
    renderApp('/login')
    await signIn(SOLICITANTE)
    expect(await screen.findByRole('heading', { name: 'Hola, Lucía' })).toBeInTheDocument()
    expect(screen.getByText('Servicios Universitarios')).toBeInTheDocument()
    expect(screen.getByText('Lucía Quispe Huamán')).toBeInTheDocument()
  })

  it('sends a supervisor to the dashboard of her area after login', async () => {
    renderApp('/login')
    await signIn(SUPERVISOR)
    expect(await screen.findByRole('heading', { name: /Dashboard · Mantenimiento e Infraestructura/ })).toBeInTheDocument()
  })

  it('shows the server message and keeps the email on a wrong password', async () => {
    renderApp('/login')
    await signIn(SOLICITANTE, 'incorrecta1')
    expect(await screen.findByText('Correo o contraseña incorrectos.')).toBeInTheDocument()
    expect(screen.getByLabelText('Correo')).toHaveValue(SOLICITANTE)
  })

  it('does not let a requester open the supervisor dashboard', async () => {
    server.use(refreshAsRequester)
    renderApp('/dashboard')
    expect(await screen.findByText('Sin permiso')).toBeInTheDocument()
    expect(screen.queryByRole('heading', { name: /Dashboard/ })).not.toBeInTheDocument()
  })

  it('shows the 404 page for an unknown route', async () => {
    server.use(refreshAsRequester)
    renderApp('/no-existe')
    expect(await screen.findByText('Página no encontrada')).toBeInTheDocument()
  })

  it('clears the session on logout and returns to the login page', async () => {
    renderApp('/login')
    const user = await signIn(SOLICITANTE)
    await screen.findByRole('heading', { name: 'Hola, Lucía' })
    await user.click(screen.getAllByRole('button', { name: 'Menú de perfil' })[0])
    await user.click(await screen.findByRole('menuitem', { name: 'Cerrar sesión' }))
    expect(await screen.findByRole('heading', { name: 'Iniciar sesión' })).toBeInTheDocument()
    expect(screen.queryByText('Lucía Quispe Huamán')).not.toBeInTheDocument()
  })

  it('restores the session from the refresh cookie on start', async () => {
    server.use(refreshAsRequester)
    renderApp('/solicitudes')
    expect(await screen.findByRole('heading', { name: 'Mis solicitudes' })).toBeInTheDocument()
  })

  it('tries to renew on a 401 and returns to login when the renewal fails', async () => {
    let refreshCalls = 0
    server.use(
      http.post('*/api/v1/auth/refresh', () => {
        refreshCalls += 1
        return new HttpResponse(null, { status: 401 })
      }),
      http.get('*/api/v1/requests', () => new HttpResponse(null, { status: 401 })),
    )
    renderApp('/login')
    await screen.findByRole('heading', { name: 'Iniciar sesión' })
    const afterBootstrap = refreshCalls
    await signIn(SOLICITANTE)
    expect(await screen.findByRole('heading', { name: 'Iniciar sesión' })).toBeInTheDocument()
    await waitFor(() => {
      // Cada consulta que recibe 401 intenta renovar; ninguna debe quedarse sin sesión válida.
      expect(refreshCalls - afterBootstrap).toBeGreaterThanOrEqual(1)
      expect(screen.queryByText('Lucía Quispe Huamán')).not.toBeInTheDocument()
    })
  })
})
