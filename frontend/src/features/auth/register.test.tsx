import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { beforeEach, describe, expect, it } from 'vitest'
import { handlers, resetMockData } from '../../mocks/handlers'
import { server } from '../../test/msw-server'
import { renderApp } from '../../test/render-app'

let registerCalls = 0

async function fill(values: { fullName?: string; email?: string; password?: string; confirm?: string }) {
  const user = userEvent.setup()
  const password = values.password ?? 'Clave12345'
  await user.type(await screen.findByLabelText('Nombre completo'), values.fullName ?? 'Ana Quispe Rojas')
  await user.type(screen.getByLabelText('Correo institucional'), values.email ?? 'ana@unmsm.edu.pe')
  await user.click(screen.getByRole('button', { name: 'Docente' }))
  await user.type(screen.getByLabelText('Contraseña'), password)
  await user.type(screen.getByLabelText('Confirmar contraseña'), values.confirm ?? password)
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
    expect(await screen.findByText('Usa tu correo institucional @unmsm.edu.pe.')).toBeInTheDocument()
    expect(registerCalls).toBe(0)
  })

  it('blocks a weak password before sending', async () => {
    renderApp('/registro')
    await fill({ password: 'solo letras' })
    expect(await screen.findByText('Incluye al menos una letra y un número.')).toBeInTheDocument()
    expect(registerCalls).toBe(0)
  })

  it('blocks mismatching passwords before sending', async () => {
    renderApp('/registro')
    await fill({ confirm: 'OtraClave123' })
    expect(await screen.findByText('Las contraseñas no coinciden.')).toBeInTheDocument()
    expect(registerCalls).toBe(0)
  })

  it('goes to login with the email filled in after a successful registration', async () => {
    renderApp('/registro')
    await fill({})
    expect(await screen.findByRole('heading', { name: 'Iniciar sesión' })).toBeInTheDocument()
    expect(screen.getByText('Cuenta creada. Ya puedes ingresar.')).toBeInTheDocument()
    expect(screen.getByLabelText('Correo')).toHaveValue('ana@unmsm.edu.pe')
    expect(registerCalls).toBe(1)
  })

  it('shows a message when the email is already registered', async () => {
    server.use(
      http.post('*/api/v1/auth/register', () =>
        HttpResponse.json({ title: 'Conflict', status: 409 }, { status: 409, headers: { 'Content-Type': 'application/problem+json' } }),
      ),
    )
    renderApp('/registro')
    await fill({})
    expect(await screen.findByText('El correo ya está registrado.')).toBeInTheDocument()
  })
})
