import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { beforeEach, describe, expect, it } from 'vitest'
import { codeOf, getDb, type MockRequest } from '../../mocks/db'
import { handlers, resetMockData } from '../../mocks/handlers'
import { tokenFor } from '../../mocks/http'
import { server } from '../../test/msw-server'
import { renderApp } from '../../test/render-app'

/** Lucía Quispe (id 1), solicitante del mock, con la sesión ya iniciada. */
function signedInAs(id: number) {
  const user = getDb().users.find((u) => u.id === id)!
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

/** Una solicitud de Lucía en ese estado, sin técnico (o la primera que haya en él). */
function requestOfLucia(status: MockRequest['status']): MockRequest {
  const r = getDb().requests.find((x) => x.requesterId === 1 && x.status === status)
  if (r) return r
  const any = getDb().requests.find((x) => x.requesterId === 1)!
  any.status = status
  any.technicianId = null
  return any
}

beforeEach(() => {
  resetMockData()
  server.use(...handlers)
  signedInAs(1)
})

describe('Cancelar y restaurar una solicitud', () => {
  it('el solicitante cancela con un motivo y puede deshacerlo al instante', async () => {
    const r = requestOfLucia('REGISTRADA')
    const user = userEvent.setup()
    renderApp(`/solicitudes/${codeOf(r)}`)

    await user.click(await screen.findByRole('button', { name: 'Cancelar solicitud' }))
    const dialog = screen.getByRole('dialog', { name: 'Cancelar solicitud' })
    expect(within(dialog).getByText(/Podrás restaurarla durante 7 días/)).toBeInTheDocument()
    await user.click(within(dialog).getByRole('button', { name: 'Cancelar solicitud' }))
    expect(await within(dialog).findByText(/al menos 5 caracteres/)).toBeInTheDocument()

    await user.type(within(dialog).getByLabelText('¿Por qué la cancelas?'), 'Ya lo arreglaron por su cuenta')
    await user.click(within(dialog).getByRole('button', { name: 'Cancelar solicitud' }))

    expect(await screen.findByRole('region', { name: 'Solicitud cancelada' })).toHaveTextContent(
      'Motivo: Ya lo arreglaron por su cuenta',
    )
    expect(r.status).toBe('CANCELADA')

    await user.click(screen.getByRole('button', { name: 'Deshacer' }))

    await waitFor(() => expect(r.status).toBe('REGISTRADA'))
    await waitFor(() => expect(screen.queryByRole('region', { name: 'Solicitud cancelada' })).not.toBeInTheDocument())
  })

  it('una cancelada se restaura al estado que tenía desde «Acciones»', async () => {
    const r = requestOfLucia('EN_EVALUACION')
    const user = userEvent.setup()
    renderApp(`/solicitudes/${codeOf(r)}`)
    await user.click(await screen.findByRole('button', { name: 'Cancelar solicitud' }))
    const dialog = screen.getByRole('dialog', { name: 'Cancelar solicitud' })
    await user.type(within(dialog).getByLabelText('¿Por qué la cancelas?'), 'Me equivoqué de aula')
    await user.click(within(dialog).getByRole('button', { name: 'Cancelar solicitud' }))
    const notice = await screen.findByRole('region', { name: 'Solicitud cancelada' })
    expect(notice).toHaveTextContent(/Puedes restaurarla hasta el/)

    await user.click(screen.getByRole('button', { name: 'Restaurar solicitud' }))
    await user.click(within(screen.getByRole('dialog', { name: 'Restaurar solicitud' })).getByRole('button', { name: 'Restaurar' }))

    await waitFor(() => expect(r.status).toBe('EN_EVALUACION'))
    expect(await screen.findByText('Solicitud restaurada')).toBeInTheDocument()
  })

  it('pasados 7 días ya no se ofrece restaurar y se explica', async () => {
    const r = requestOfLucia('REGISTRADA')
    r.status = 'CANCELADA'
    r.history.push({
      fromStatus: 'REGISTRADA',
      toStatus: 'CANCELADA',
      actorName: 'Lucía Quispe Huamán',
      reason: 'Cancelada por el solicitante: Ya no hace falta',
      createdAt: Date.now() - 8 * 86_400_000,
      note: undefined,
    })
    renderApp(`/solicitudes/${codeOf(r)}`)

    expect(await screen.findByRole('region', { name: 'Solicitud cancelada' })).toHaveTextContent(
      'ya no se puede restaurar',
    )
    expect(screen.queryByRole('button', { name: 'Restaurar solicitud' })).not.toBeInTheDocument()
  })

  it('con un técnico asignado ya no se puede cancelar', async () => {
    const r = requestOfLucia('ASIGNADA')
    renderApp(`/solicitudes/${codeOf(r)}`)

    expect(await screen.findByRole('heading', { name: codeOf(r) })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Cancelar solicitud' })).not.toBeInTheDocument()
  })
})
