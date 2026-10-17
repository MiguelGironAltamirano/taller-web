import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { beforeEach, describe, expect, it } from 'vitest'
import { handlers, resetMockData } from '../../mocks/handlers'
import { server } from '../../test/msw-server'
import { renderApp } from '../../test/render-app'

/** Sesión de la solicitante (la cookie de renovación simulada devuelve su usuario). */
const refreshAsRequester = http.post('*/api/v1/auth/refresh', () =>
  HttpResponse.json({
    accessToken: 'mock-token-1',
    expiresIn: 900,
    user: { id: 1, fullName: 'Lucía Quispe Huamán', email: 'lquispe@unmsm.edu.pe', role: 'SOLICITANTE', areaId: null },
  }),
)

let createCalls = 0
const countCreates = http.post('*/api/v1/requests', () => {
  createCalls += 1
  return undefined // deja pasar la petición al mock real
})

/** Los catálogos llegan de la API: se espera a que el selector tenga sus opciones. */
const catalogsLoaded = () => screen.findByRole('option', { name: 'Electricidad' })

async function fillValidForm(description: string) {
  const user = userEvent.setup()
  await catalogsLoaded()
  await user.selectOptions(screen.getByLabelText('Categoría *'), 'Electricidad')
  await user.selectOptions(screen.getByLabelText('Facultad *'), 'Facultad de Medicina')
  await user.selectOptions(screen.getByLabelText('Pabellón *'), 'Pabellón Central')
  await user.selectOptions(screen.getByLabelText('Ambiente *'), 'Aula 12')
  await user.type(screen.getByLabelText('Descripción *'), description)
  await user.click(screen.getByRole('radio', { name: /Alta/ }))
  return user
}

describe('Nueva solicitud', () => {
  beforeEach(() => {
    resetMockData()
    createCalls = 0
    server.use(refreshAsRequester, countCreates, ...handlers)
  })

  it('muestra el código generado y la solicitud aparece en la lista', async () => {
    renderApp('/solicitudes/nueva')
    const user = await fillValidForm('El tomacorriente del aula no funciona desde ayer')
    await user.click(screen.getByRole('button', { name: 'Registrar solicitud' }))

    expect(await screen.findByRole('heading', { name: 'Solicitud registrada' })).toBeInTheDocument()
    const code = (await screen.findByText(/^SOL-\d{4}-\d{6}$/)).textContent!
    expect(createCalls).toBe(1)

    await user.click(screen.getByRole('link', { name: /Mis solicitudes/ }))
    expect(await screen.findByText(code)).toBeInTheDocument()
  })

  it('no envía el formulario con la descripción fuera de rango', async () => {
    renderApp('/solicitudes/nueva')
    const user = await fillValidForm('muy corta')
    await user.click(screen.getByRole('button', { name: 'Registrar solicitud' }))

    expect(await screen.findByText(/La descripción debe tener al menos 20 caracteres \(9\/20\)/)).toBeInTheDocument()
    expect(screen.queryByRole('heading', { name: 'Solicitud registrada' })).not.toBeInTheDocument()
    expect(createCalls).toBe(0)
  })

  it('no envía el formulario si faltan datos obligatorios', async () => {
    renderApp('/solicitudes/nueva')
    const user = userEvent.setup()
    await user.click(await screen.findByRole('button', { name: 'Registrar solicitud' }))

    expect(await screen.findByText('Selecciona una categoría.')).toBeInTheDocument()
    expect(screen.getByText('Selecciona la facultad.')).toBeInTheDocument()
    expect(screen.getByText('Selecciona una prioridad.')).toBeInTheDocument()
    expect(createCalls).toBe(0)
  })

  it('el área responsable sale de la categoría elegida', async () => {
    renderApp('/solicitudes/nueva')
    const user = userEvent.setup()
    await catalogsLoaded()
    await user.selectOptions(screen.getByLabelText('Categoría *'), 'Red e internet')
    expect(await screen.findByText(/Área responsable:/)).toHaveTextContent('Área responsable: Soporte Informático')
  })
})
