import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { beforeEach, describe, expect, it } from 'vitest'
import { handlers, resetMockData } from '../mocks/handlers'
import { server } from '../test/msw-server'
import { renderApp } from '../test/render-app'

const info = http.get('*/api/v1/system/info', () => HttpResponse.json({ name: 'servicios', version: '0.0.1-SNAPSHOT' }))

async function signIn(email: string, password = 'Demo12345!') {
  const user = userEvent.setup()
  await user.type(await screen.findByLabelText('Correo institucional'), email)
  await user.type(screen.getByLabelText('Contraseña'), password)
  await user.click(screen.getByRole('button', { name: 'Iniciar sesión' }))
  return user
}

describe('App', () => {
  beforeEach(() => {
    resetMockData()
    server.use(info, ...handlers)
  })

  it('redirects to the login page when there is no session', async () => {
    renderApp('/')
    expect(await screen.findByRole('heading', { name: 'Iniciar sesión' })).toBeInTheDocument()
  })

  it('sends a requester to "Mis solicitudes" after login and shows the API version', async () => {
    renderApp('/login')
    await signIn('estudiante@unmsm.edu.pe')
    expect(await screen.findByRole('heading', { name: 'Mis solicitudes' })).toBeInTheDocument()
    expect(screen.getByText('Servicios Universitarios')).toBeInTheDocument()
    expect(screen.getByText('Estudiante Demo')).toBeInTheDocument()
    expect(await screen.findByText('Versión 0.0.1-SNAPSHOT')).toBeInTheDocument()
  })

  it('sends a supervisor to "Bandeja de solicitudes" after login', async () => {
    renderApp('/login')
    await signIn('supervisor.mantenimiento@unmsm.edu.pe')
    expect(await screen.findByRole('heading', { name: 'Bandeja de solicitudes' })).toBeInTheDocument()
  })

  it('shows "Credenciales inválidas" and keeps the email on a wrong password', async () => {
    renderApp('/login')
    await signIn('estudiante@unmsm.edu.pe', 'incorrecta1')
    expect(await screen.findByText('Credenciales inválidas')).toBeInTheDocument()
    expect(screen.getByLabelText('Correo institucional')).toHaveValue('estudiante@unmsm.edu.pe')
  })

  it('does not let a requester open the supervisor inbox', async () => {
    server.use(
      http.post('*/api/v1/auth/refresh', () =>
        HttpResponse.json({
          accessToken: 'mock-token-4',
          expiresIn: 900,
          user: { id: 4, fullName: 'Estudiante Demo', email: 'estudiante@unmsm.edu.pe', role: 'SOLICITANTE', areaId: null },
        }),
      ),
    )
    renderApp('/bandeja')
    expect(await screen.findByText('No tienes permiso para ver esta página')).toBeInTheDocument()
    expect(screen.queryByRole('heading', { name: 'Bandeja de solicitudes' })).not.toBeInTheDocument()
  })

  it('clears the session on logout and returns to the login page', async () => {
    renderApp('/login')
    const user = await signIn('estudiante@unmsm.edu.pe')
    await screen.findByRole('heading', { name: 'Mis solicitudes' })
    await user.click(screen.getByRole('button', { name: 'Cerrar sesión' }))
    expect(await screen.findByRole('heading', { name: 'Iniciar sesión' })).toBeInTheDocument()
    expect(screen.queryByText('Estudiante Demo')).not.toBeInTheDocument()
  })

  it('restores the session from the refresh cookie on start', async () => {
    server.use(
      http.post('*/api/v1/auth/refresh', () =>
        HttpResponse.json({
          accessToken: 'mock-token-4',
          expiresIn: 900,
          user: { id: 4, fullName: 'Estudiante Demo', email: 'estudiante@unmsm.edu.pe', role: 'SOLICITANTE', areaId: null },
        }),
      ),
    )
    renderApp('/solicitudes')
    expect(await screen.findByRole('heading', { name: 'Mis solicitudes' })).toBeInTheDocument()
  })

  it('renews once on a 401 and returns to login when the renewal fails', async () => {
    let refreshCalls = 0
    server.use(
      http.post('*/api/v1/auth/refresh', () => {
        refreshCalls += 1
        return new HttpResponse(null, { status: 401 })
      }),
      http.get('*/api/v1/system/info', () => new HttpResponse(null, { status: 401 })),
    )
    renderApp('/login')
    await screen.findByRole('heading', { name: 'Iniciar sesión' })
    const afterBootstrap = refreshCalls
    await signIn('estudiante@unmsm.edu.pe')
    expect(await screen.findByRole('heading', { name: 'Iniciar sesión' })).toBeInTheDocument()
    await waitFor(() => {
      expect(refreshCalls - afterBootstrap).toBe(1)
      expect(screen.queryByText('Estudiante Demo')).not.toBeInTheDocument()
      expect(screen.getByRole('heading', { name: 'Iniciar sesión' })).toBeInTheDocument()
    })
  })
})
