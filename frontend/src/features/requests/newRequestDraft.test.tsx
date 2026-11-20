import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { beforeEach, describe, expect, it } from 'vitest'
import { getDb } from '../../mocks/db'
import { handlers, resetMockData } from '../../mocks/handlers'
import { tokenFor } from '../../mocks/http'
import { server } from '../../test/msw-server'
import { renderApp } from '../../test/render-app'

/** Lucía Quispe (id 1), solicitante del mock, con la sesión ya iniciada. */
const LUCIA_ID = 1

function signedIn() {
  const user = getDb().users.find((u) => u.id === LUCIA_ID)!
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

const DESCRIPTION = 'El foco del pasadizo parpadea desde ayer en la noche'
const savedDraft = () => getDb().drafts[LUCIA_ID]

function leaveDraft(fields: Partial<{ category: string; description: string; priority: string }>) {
  getDb().drafts[LUCIA_ID] = {
    category: '',
    description: '',
    priority: '',
    faculty: '',
    pavilion: '',
    room: '',
    ...fields,
    updatedAt: new Date('2026-11-20T15:30:00Z').toISOString(),
  }
}

beforeEach(() => {
  resetMockData()
  server.use(...handlers)
  signedIn()
})

describe('Borrador de «Nueva solicitud»', () => {
  it('guarda lo escrito sin que la persona tenga que hacer nada', async () => {
    const user = userEvent.setup()
    renderApp('/solicitudes/nueva')

    await user.type(await screen.findByLabelText(/^Descripción/), DESCRIPTION)

    await waitFor(() => expect(savedDraft()?.description).toBe(DESCRIPTION), { timeout: 4000 })
    expect(await screen.findByText(/Borrador guardado/)).toBeInTheDocument()
  })

  it('al cancelar no se pierde lo escrito: se guarda y se avisa dónde retomarlo', async () => {
    const user = userEvent.setup()
    renderApp('/solicitudes/nueva')
    await user.type(await screen.findByLabelText(/^Descripción/), 'Gotea el caño')

    await user.click(screen.getByRole('button', { name: 'Cancelar' }))

    expect(await screen.findByRole('heading', { name: 'Mis solicitudes' })).toBeInTheDocument()
    expect(screen.getByText('Guardamos tu borrador')).toBeInTheDocument()
    await waitFor(() => expect(savedDraft()?.description).toBe('Gotea el caño'))
  })

  it('al cancelar sin haber escrito nada no deja borrador', async () => {
    const user = userEvent.setup()
    renderApp('/solicitudes/nueva')

    await user.click(await screen.findByRole('button', { name: 'Cancelar' }))

    expect(await screen.findByRole('heading', { name: 'Mis solicitudes' })).toBeInTheDocument()
    expect(savedDraft()).toBeUndefined()
  })

  it('ofrece retomar el borrador y lo deja como estaba, avisando de las evidencias', async () => {
    leaveDraft({ category: 'Electricidad', description: DESCRIPTION, priority: 'Alta' })
    const user = userEvent.setup()
    renderApp('/solicitudes/nueva')

    const offer = await screen.findByRole('region', { name: 'Borrador sin enviar' })
    await user.click(within(offer).getByRole('button', { name: 'Retomar' }))

    expect(screen.getByLabelText(/^Descripción/)).toHaveValue(DESCRIPTION)
    expect(screen.getByLabelText(/^Categoría/)).toHaveValue('Electricidad')
    expect(screen.getByText(/Las evidencias no se guardan en el borrador/)).toBeInTheDocument()
    expect(screen.queryByRole('region', { name: 'Borrador sin enviar' })).not.toBeInTheDocument()
  })

  it('descartar el borrador lo elimina', async () => {
    leaveDraft({ description: DESCRIPTION })
    const user = userEvent.setup()
    renderApp('/solicitudes/nueva')

    const offer = await screen.findByRole('region', { name: 'Borrador sin enviar' })
    await user.click(within(offer).getByRole('button', { name: 'Descartar' }))

    await waitFor(() => expect(savedDraft()).toBeUndefined())
    expect(screen.getByLabelText(/^Descripción/)).toHaveValue('')
  })

  it('el inicio recuerda el borrador y lleva a retomarlo', async () => {
    leaveDraft({ description: DESCRIPTION })
    const user = userEvent.setup()
    renderApp('/inicio')

    const card = await screen.findByRole('region', { name: 'Borrador sin enviar' })
    await user.click(within(card).getByRole('link', { name: 'Retomar borrador' }))

    expect(await screen.findByLabelText(/^Descripción/)).toHaveValue(DESCRIPTION)
  })

  it('registrar la solicitud descarta el borrador', async () => {
    leaveDraft({ description: DESCRIPTION })
    const user = userEvent.setup()
    renderApp('/solicitudes/nueva')
    const offer = await screen.findByRole('region', { name: 'Borrador sin enviar' })
    await user.click(within(offer).getByRole('button', { name: 'Retomar' }))

    await user.selectOptions(screen.getByLabelText(/^Categoría/), 'Electricidad')
    await user.click(screen.getByRole('radio', { name: /^Alta/ }))
    const faculty = screen.getByLabelText(/^Facultad/)
    await user.selectOptions(faculty, within(faculty).getAllByRole('option')[1])
    const pavilion = screen.getByLabelText(/^Pabellón/)
    await user.selectOptions(pavilion, within(pavilion).getAllByRole('option')[1])
    const room = screen.getByLabelText(/^Ambiente/)
    await user.selectOptions(room, within(room).getAllByRole('option')[1])
    await user.click(screen.getByRole('button', { name: 'Registrar solicitud' }))

    expect(await screen.findByText(/registrada/)).toBeInTheDocument()
    expect(savedDraft()).toBeUndefined()
    // Recorre todo el formulario: con la máquina cargada supera los 5 s por defecto.
  }, 15_000)
})

describe('Deshacer la sugerencia de categoría y prioridad', () => {
  it('aceptar la sugerencia se puede deshacer y vuelve lo que había', async () => {
    const user = userEvent.setup()
    renderApp('/solicitudes/nueva')
    await screen.findByRole('option', { name: 'Mobiliario y carpintería' })
    await user.selectOptions(screen.getByLabelText(/^Categoría/), 'Mobiliario y carpintería')
    await user.type(screen.getByLabelText(/^Descripción/), DESCRIPTION)

    await user.click(await screen.findByRole('button', { name: 'Aceptar' }))
    expect(screen.getByLabelText(/^Categoría/)).toHaveValue('Electricidad')

    await user.click(screen.getByRole('button', { name: 'Deshacer' }))
    expect(screen.getByLabelText(/^Categoría/)).toHaveValue('Mobiliario y carpintería')
    expect(screen.getByLabelText(/^Descripción/)).toHaveValue(DESCRIPTION)
  })
})
