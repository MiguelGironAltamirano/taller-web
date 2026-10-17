import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { beforeEach, describe, expect, it } from 'vitest'
import { handlers, resetMockData } from '../../mocks/handlers'
import { server } from '../../test/msw-server'
import { renderApp } from '../../test/render-app'

let registerCalls = 0

async function fill(values: { fullName?: string; email?: string; password?: string }) {
  const user = userEvent.setup()
  await user.type(await screen.findByLabelText('Nombre completo'), values.fullName ?? 'Ana Quispe Rojas')
  await user.type(screen.getByLabelText('Correo institucional'), values.email ?? 'ana@unmsm.edu.pe')
  await user.type(screen.getByLabelText('Contraseña'), values.password ?? 'Clave12345')
  await user.selectOptions(screen.getByLabelText('Tipo de solicitante'), 'DOCENTE')
  await user.click(screen.getByRole('button', { name: 'Crear cuenta' }))
}

describe('RegisterPage', () => {
  beforeEach(() => {
    resetMockData()
    registerCalls = 0
    server.use(
      http.post('*/api/v1/auth/register', () => {
        registerCalls += 1
        return HttpResponse.json({}, { status: 201 })
      }),
      ...handlers,
    )
  })

  it('blocks a non-UNMSM email before sending', async () => {
    renderApp('/registro')
    await fill({ email: 'ana@gmail.com' })
    expect(await screen.findByText('El correo debe terminar en @unmsm.edu.pe')).toBeInTheDocument()
    expect(registerCalls).toBe(0)
  })

  it('blocks a weak password before sending', async () => {
    renderApp('/registro')
    await fill({ password: 'solo letras' })
    expect(await screen.findByText('La contraseña debe tener entre 8 y 72 caracteres, con al menos una letra y un dígito')).toBeInTheDocument()
    expect(registerCalls).toBe(0)
  })

  it('goes to login with a confirmation after a successful registration', async () => {
    renderApp('/registro')
    await fill({})
    expect(await screen.findByText('Cuenta creada. Ahora puedes iniciar sesión.')).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Iniciar sesión' })).toBeInTheDocument()
    expect(registerCalls).toBe(1)
  })

  it('shows a message when the email is already registered', async () => {
    server.use(http.post('*/api/v1/auth/register', () => HttpResponse.json({ title: 'Conflict', status: 409 }, { status: 409, headers: { 'Content-Type': 'application/problem+json' } })))
    renderApp('/registro')
    await fill({})
    expect(await screen.findByText('El correo ya está registrado')).toBeInTheDocument()
  })
})
