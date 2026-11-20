import { screen } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { beforeEach, describe, expect, it } from 'vitest'
import { getDb } from '../mocks/db'
import { handlers, resetMockData } from '../mocks/handlers'
import { tokenFor } from '../mocks/http'
import { server } from '../test/msw-server'
import { renderApp } from '../test/render-app'

/** Abre la aplicación con la sesión de `email` ya iniciada (la cookie de renovación del mock). */
function signedIn(email: string) {
  const user = getDb().users.find((u) => u.email === email)!
  server.use(
    http.post('*/api/v1/auth/refresh', () =>
      HttpResponse.json({
        accessToken: tokenFor(user),
        expiresIn: 900,
        user: { id: user.id, fullName: user.fullName, email: user.email, role: user.role, areaId: user.areaId },
      }),
    ),
  )
}

/** El servidor cae: nginx responde 503 con problem details en español. */
const unavailable = () =>
  HttpResponse.json(
    {
      type: 'about:blank',
      title: 'Service Unavailable',
      status: 503,
      detail: 'El servicio no está disponible en este momento. Inténtalo de nuevo en unos minutos',
    },
    { status: 503, headers: { 'Content-Type': 'application/problem+json' } },
  )

/** Sin conexión: `fetch` falla antes de obtener una respuesta. */
const offline = () => HttpResponse.error()

const ADMIN = 'rsalazar@unmsm.edu.pe'
const SUPERVISOR = getDbEmail('SUPERVISOR')
const REQUESTER = 'lquispe@unmsm.edu.pe'

function getDbEmail(role: string) {
  return getDb().users.find((u) => u.role === role && u.active && u.areaId !== null)!.email
}

beforeEach(() => {
  resetMockData()
  server.use(...handlers)
})

describe('Estados de error de las pantallas', () => {
  it.each([
    ['/admin/usuarios', ADMIN, '*/api/v1/admin/users', 'No pudimos cargar los usuarios', 'Sin resultados'],
    ['/admin/auditoria', ADMIN, '*/api/v1/admin/audit', 'No pudimos cargar la auditoría', 'Sin resultados'],
    ['/admin/triage', ADMIN, '*/api/v1/admin/triage-rules', 'No pudimos cargar las reglas', 'Aún no hay reglas'],
    ['/equipo', SUPERVISOR, '*/api/v1/dashboard/team', 'No pudimos cargar el equipo', 'Sin técnicos en tu área'],
    ['/notificaciones', REQUESTER, '*/api/v1/notifications', 'No pudimos cargar tus notificaciones', 'No tienes notificaciones.'],
  ])('%s explains the failure instead of looking empty', async (path, email, endpoint, title, empty) => {
    signedIn(email)
    server.use(http.get(endpoint, unavailable))
    renderApp(path)

    const alert = await screen.findByRole('alert', {}, { timeout: 8000 })
    expect(alert).toHaveTextContent(title)
    expect(alert).toHaveTextContent('El servicio no está disponible en este momento.')
    expect(screen.getByRole('button', { name: 'Reintentar' })).toBeInTheDocument()
    expect(screen.queryByText(empty)).not.toBeInTheDocument()
  })

  it('the dashboard stops loading and explains the failure', async () => {
    signedIn(ADMIN)
    server.use(http.get('*/api/v1/dashboard/indicators', unavailable))
    renderApp('/dashboard')

    expect(await screen.findByRole('alert', {}, { timeout: 8000 })).toHaveTextContent('No pudimos cargar los indicadores')
    expect(screen.queryByRole('status', { name: 'Cargando indicadores' })).not.toBeInTheDocument()
  }, 15_000)

  it('without connection the message says so', async () => {
    signedIn(ADMIN)
    server.use(http.get('*/api/v1/admin/config', offline))
    renderApp('/admin/configuracion')

    expect(await screen.findByRole('alert', {}, { timeout: 8000 })).toHaveTextContent(
      'No pudimos conectar con el servidor. Revisa tu conexión e inténtalo de nuevo.',
    )
  })

  it('a new request warns when the categories cannot be loaded', async () => {
    signedIn(REQUESTER)
    server.use(http.get('*/api/v1/catalogs', unavailable))
    renderApp('/solicitudes/nueva')

    expect(await screen.findByRole('alert', {}, { timeout: 8000 })).toHaveTextContent(
      'No pudimos cargar las categorías, prioridades y ubicaciones',
    )
  })
})
