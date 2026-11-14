import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { beforeEach, describe, expect, it } from 'vitest'
import { getDb } from '../../mocks/db'
import { handlers, resetMockData } from '../../mocks/handlers'
import { server } from '../../test/msw-server'
import { renderApp } from '../../test/render-app'

/** Sesión del administrador: ve todas las solicitudes, así que el listado tiene varias páginas. */
const refreshAsAdmin = () => {
  const admin = getDb().users.find((u) => u.role === 'ADMINISTRADOR')!
  return http.post('*/api/v1/auth/refresh', () =>
    HttpResponse.json({
      accessToken: `mock-token-${admin.id}`,
      expiresIn: 900,
      user: { id: admin.id, fullName: admin.fullName, email: admin.email, role: 'ADMINISTRADOR', areaId: null },
    }),
  )
}

/** Registra los parámetros de cada `GET /requests` y deja pasar la petición al mock real. */
let listCalls: URLSearchParams[] = []
const spyList = http.get('*/api/v1/requests', ({ request }) => {
  listCalls.push(new URL(request.url).searchParams)
  return undefined
})
/** La última petición de la bandeja (las de 500 son de Inicio y la paleta de comandos). */
const lastCall = () => listCalls.filter((p) => p.get('size') !== '500').at(-1)!

describe('Bandeja con filtros y paginación del servidor', () => {
  beforeEach(() => {
    resetMockData()
    listCalls = []
    server.use(refreshAsAdmin(), spyList, ...handlers)
  })

  it('pide una página al servidor y avanza con "Siguiente"', async () => {
    renderApp('/solicitudes')
    const total = getDb().requests.length
    expect(await screen.findByText(`1–10 de ${total}`)).toBeInTheDocument()
    expect(lastCall().get('page')).toBe('0')
    expect(lastCall().get('size')).toBe('10')
    expect(lastCall().get('sort')).toBe('due')

    await userEvent.setup().click(screen.getByRole('button', { name: 'Siguiente' }))
    expect(await screen.findByText(`11–${Math.min(20, total)} de ${total}`)).toBeInTheDocument()
    expect(lastCall().get('page')).toBe('1')
  })

  it('envía la categoría por id y vuelve a la primera página', async () => {
    const user = userEvent.setup()
    renderApp('/solicitudes?page=1')
    await screen.findByRole('option', { name: 'Electricidad' })
    const electricity = getDb().categories.find((c) => c.name === 'Electricidad')!

    await user.selectOptions(screen.getByLabelText('Categoría'), 'Electricidad')

    await waitFor(() => expect(lastCall().get('categoryId')).toBe(String(electricity.id)))
    expect(lastCall().get('page')).toBe('0')
    expect(await screen.findByRole('button', { name: 'Quitar filtro Categoría: Electricidad' })).toBeInTheDocument()
  })

  it('traduce las vistas guardadas a parámetros del servidor', async () => {
    renderApp('/solicitudes?view=confirm')
    await waitFor(() => expect(lastCall()?.get('status')).toBe('RESUELTA'))
    const resolved = getDb().requests.filter((r) => r.status === 'RESUELTA').length
    expect(await screen.findByText(`${resolved} de ${getDb().requests.length} solicitudes`)).toBeInTheDocument()
  })

  it('no consulta cuando la combinación de filtros no puede devolver nada', async () => {
    renderApp('/solicitudes?view=open&status=Cerrada')
    expect(await screen.findByText('Sin resultados')).toBeInTheDocument()
    expect(listCalls.filter((p) => p.get('size') === '10')).toHaveLength(0)
  })

  it('si la página no carga, ofrece reintentar y muestra las solicitudes al lograrlo', async () => {
    let failures = 1
    server.use(
      http.get('*/api/v1/requests', ({ request }) => {
        if (new URL(request.url).searchParams.get('size') === '10' && failures > 0) {
          failures -= 1
          return HttpResponse.json({ title: 'Error', status: 500 }, { status: 500 })
        }
        return undefined
      }),
    )
    renderApp('/solicitudes')

    const alert = await screen.findByRole('alert')
    expect(alert).toHaveTextContent('No pudimos cargar las solicitudes')
    await userEvent.click(within(alert).getByRole('button', { name: 'Reintentar' }))

    const total = getDb().requests.length
    expect(await screen.findByText(`${total} de ${total} solicitudes`)).toBeInTheDocument()
    expect(screen.queryByText('No pudimos cargar las solicitudes')).not.toBeInTheDocument()
  })

  it('los contadores por estado vienen de /requests/counts', async () => {
    renderApp('/solicitudes')
    const resolved = getDb().requests.filter((r) => r.status === 'RESUELTA').length
    const button = await screen.findByRole('button', { name: new RegExp(`^Resuelta\\s*${resolved}$`) })
    expect(within(button).getByText(String(resolved))).toBeInTheDocument()
  })
})
