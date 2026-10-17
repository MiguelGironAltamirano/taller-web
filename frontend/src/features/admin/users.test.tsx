import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it } from 'vitest'
import { handlers, resetMockData } from '../../mocks/handlers'
import { server } from '../../test/msw-server'
import { renderApp } from '../../test/render-app'

const ADMIN = 'rsalazar@unmsm.edu.pe'

async function signInAsAdmin() {
  const user = userEvent.setup()
  await user.type(await screen.findByLabelText('Correo'), ADMIN)
  await user.type(screen.getByLabelText('Contraseña'), 'Demo12345!')
  await user.click(screen.getByRole('button', { name: 'Ingresar' }))
  return user
}

describe('Usuarios (administración)', () => {
  beforeEach(() => {
    resetMockData()
    server.use(...handlers)
  })

  it('muestra una sola vez la contraseña temporal del usuario creado', async () => {
    renderApp('/admin/usuarios')
    const user = await signInAsAdmin()

    await user.click(await screen.findByRole('button', { name: 'Nuevo usuario' }))
    await user.type(screen.getByLabelText('Nombre completo'), 'Nuevo Técnico')
    await user.type(screen.getByLabelText('Correo institucional'), 'ntecnico@unmsm.edu.pe')
    await user.selectOptions(screen.getByLabelText('Rol'), 'TECNICO')
    await user.selectOptions(screen.getByLabelText(/^Área/), screen.getAllByRole('option', { name: /Mantenimiento/ })[0])
    await user.click(screen.getByRole('button', { name: 'Guardar' }))

    const dialog = await screen.findByRole('dialog', { name: 'Usuario creado' })
    expect(within(dialog).getByText(/ntecnico@unmsm\.edu\.pe/)).toBeInTheDocument()
    expect(within(dialog).getByTestId('temporary-password').textContent).toMatch(/^[A-Za-z0-9]{12}$/)
    expect(within(dialog).getByText(/no se volverá a mostrar/i)).toBeInTheDocument()

    await user.click(within(dialog).getByRole('button', { name: 'Entendido' }))
    expect(screen.queryByRole('dialog', { name: 'Usuario creado' })).not.toBeInTheDocument()
    expect(screen.queryByTestId('temporary-password')).not.toBeInTheDocument()
  })

  it('avisa cuando el servidor rechaza la desactivación de la propia cuenta', async () => {
    renderApp('/admin/usuarios')
    const user = await signInAsAdmin()

    const ownRow = (await screen.findByText(ADMIN)).closest('[role="row"]') as HTMLElement
    await user.click(within(ownRow).getByRole('button', { name: 'Desactivar' }))

    expect(await screen.findByText('No puedes desactivar tu propia cuenta')).toBeInTheDocument()
    expect(within(ownRow).getByText('Activo')).toBeInTheDocument()
  })
})
