import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import type { Priority, WorkflowActionId } from '../../shared/domain'
import { ActionModal } from './ActionModal'
import { mockWorkflowApi, problem, renderWithProviders, row } from './test-support'

const REGISTRADA = row()
const ASIGNADA = row({ state: 'Asignada', technician: 'Tomás Técnico' })

function open(action: WorkflowActionId, target = REGISTRADA) {
  const onClose = vi.fn()
  const view = renderWithProviders(<ActionModal action={action} target={target} onClose={onClose} />)
  return { onClose, ...view }
}

const submit = (name: string) => userEvent.click(screen.getByRole('button', { name }))

describe('ActionModal: acciones sin datos (evaluar, iniciar atención)', () => {
  it('muestra el mensaje de la acción y envía solo el estado destino', async () => {
    const { calls } = mockWorkflowApi()
    const { onClose } = open('eval')

    expect(await screen.findByRole('dialog', { name: 'Pasar a evaluación' })).toBeInTheDocument()
    expect(screen.getByText('La solicitud pasará a En evaluación.')).toBeInTheDocument()
    expect(screen.queryByRole('combobox')).not.toBeInTheDocument()
    expect(screen.queryByRole('textbox')).not.toBeInTheDocument()

    await submit('Pasar a evaluación')

    expect(await screen.findByText('Pasar a evaluación: listo. Historial y notificación actualizados.')).toBeInTheDocument()
    expect(calls).toEqual([{ path: 'SOL-2026-000001/transitions', body: { to: 'EN_EVALUACION' } }])
    expect(onClose).toHaveBeenCalledTimes(1)
  })

  it('iniciar atención envía EN_ATENCION', async () => {
    const { calls } = mockWorkflowApi()
    const { onClose } = open('start', ASIGNADA)

    await submit('Iniciar atención')

    await waitFor(() => expect(onClose).toHaveBeenCalled())
    expect(calls).toEqual([{ path: 'SOL-2026-000001/transitions', body: { to: 'EN_ATENCION' } }])
  })
})

describe('ActionModal: asignar técnico', () => {
  it('pide los técnicos del área de la solicitud y muestra su carga', async () => {
    const { technicianAreas } = mockWorkflowApi()
    open('assign')

    expect(await screen.findByRole('option', { name: 'Tomás Técnico · 2 solicitudes activas' })).toBeInTheDocument()
    expect(screen.getByRole('option', { name: 'Teresa Técnica · 0 solicitudes activas' })).toBeInTheDocument()
    expect(technicianAreas).toEqual(['1']) // Mantenimiento
  })

  it('exige elegir un técnico y no llama a la API sin él', async () => {
    const { calls } = mockWorkflowApi()
    const { onClose } = open('assign')
    await screen.findByRole('option', { name: /Tomás Técnico/ })

    await submit('Asignar')

    expect(await screen.findByText('Selecciona un técnico.')).toBeInTheDocument()
    expect(calls).toEqual([])
    expect(onClose).not.toHaveBeenCalled()
  })

  it('envía ASIGNADA con el técnico elegido', async () => {
    const { calls } = mockWorkflowApi()
    const { onClose } = open('assign')
    await screen.findByRole('option', { name: /Teresa Técnica/ })

    await userEvent.selectOptions(screen.getByRole('combobox', { name: 'Técnico del área' }), 'Teresa Técnica · 0 solicitudes activas')
    await submit('Asignar')

    await waitFor(() => expect(onClose).toHaveBeenCalled())
    expect(calls).toEqual([{ path: 'SOL-2026-000001/transitions', body: { to: 'ASIGNADA', technicianId: 4 } }])
  })
})

describe('ActionModal: reasignar técnico', () => {
  it('no ofrece al técnico que ya tiene la solicitud', async () => {
    mockWorkflowApi()
    open('reassign', ASIGNADA)

    expect(await screen.findByRole('option', { name: /Teresa Técnica/ })).toBeInTheDocument()
    expect(screen.queryByRole('option', { name: /Tomás Técnico/ })).not.toBeInTheDocument()
  })

  it('exige técnico y motivo', async () => {
    const { calls } = mockWorkflowApi()
    open('reassign', ASIGNADA)
    await screen.findByRole('option', { name: /Teresa Técnica/ })

    await submit('Reasignar')

    expect(await screen.findByText('Selecciona un técnico.')).toBeInTheDocument()
    expect(screen.getByText('Indica un motivo de al menos 5 caracteres.')).toBeInTheDocument()
    expect(calls).toEqual([])
  })

  it('un motivo de solo espacios no cuenta', async () => {
    const { calls } = mockWorkflowApi()
    open('reassign', ASIGNADA)
    await screen.findByRole('option', { name: /Teresa Técnica/ })

    await userEvent.selectOptions(screen.getByRole('combobox', { name: 'Técnico del área' }), 'Teresa Técnica · 0 solicitudes activas')
    await userEvent.type(screen.getByRole('textbox', { name: 'Motivo de la reasignación' }), '       ')
    await submit('Reasignar')

    expect(await screen.findByText('Indica un motivo de al menos 5 caracteres.')).toBeInTheDocument()
    expect(calls).toEqual([])
  })

  it('envía técnico y motivo sin espacios sobrantes', async () => {
    const { calls } = mockWorkflowApi()
    const { onClose } = open('reassign', ASIGNADA)
    await screen.findByRole('option', { name: /Teresa Técnica/ })

    await userEvent.selectOptions(screen.getByRole('combobox', { name: 'Técnico del área' }), 'Teresa Técnica · 0 solicitudes activas')
    await userEvent.type(screen.getByRole('textbox', { name: 'Motivo de la reasignación' }), '  Está de licencia  ')
    await submit('Reasignar')

    await waitFor(() => expect(onClose).toHaveBeenCalled())
    expect(calls).toEqual([
      { path: 'SOL-2026-000001/transitions', body: { to: 'ASIGNADA', technicianId: 4, reason: 'Está de licencia' } },
    ])
  })
})

describe('ActionModal: resolver', () => {
  const EN_ATENCION = row({ state: 'En atención', technician: 'Tomás Técnico' })

  it('exige el comentario de solución', async () => {
    const { calls } = mockWorkflowApi()
    open('resolve', EN_ATENCION)

    await submit('Marcar como resuelta')

    expect(await screen.findByText('Indica un motivo de al menos 5 caracteres.')).toBeInTheDocument()
    expect(calls).toEqual([])
  })

  it('envía RESUELTA con el comentario de solución', async () => {
    const { calls } = mockWorkflowApi()
    const { onClose } = open('resolve', EN_ATENCION)

    await userEvent.type(screen.getByRole('textbox', { name: 'Comentario de solución' }), 'Se cambió el tomacorriente')
    await submit('Marcar como resuelta')

    await waitFor(() => expect(onClose).toHaveBeenCalled())
    expect(calls).toEqual([
      { path: 'SOL-2026-000001/transitions', body: { to: 'RESUELTA', reason: 'Se cambió el tomacorriente' } },
    ])
  })
})

describe('ActionModal: cerrar como supervisor', () => {
  it.each([
    ['noproc', 'Cerrar como No procede', 'NO_PROCEDE'],
    ['dup', 'Cerrar como Duplicada', 'DUPLICADA'],
  ] as const)('%s exige motivo y envía el motivo de cierre %s', async (action, title, closureReason) => {
    const { calls } = mockWorkflowApi()
    const { onClose } = open(action)
    expect(screen.getByRole('dialog', { name: title })).toBeInTheDocument()

    await submit('Cerrar solicitud')
    expect(await screen.findByText('Indica un motivo de al menos 5 caracteres.')).toBeInTheDocument()
    expect(calls).toEqual([])

    await userEvent.type(screen.getByRole('textbox'), 'Ya existe SOL-2026-000000')
    await submit('Cerrar solicitud')

    await waitFor(() => expect(onClose).toHaveBeenCalled())
    expect(calls).toEqual([
      { path: 'SOL-2026-000001/transitions', body: { to: 'CERRADA', closureReason, reason: 'Ya existe SOL-2026-000000' } },
    ])
  })
})

describe('ActionModal: cambiar prioridad', () => {
  it('parte de la prioridad actual y muestra el nuevo vencimiento al cambiarla', async () => {
    mockWorkflowApi()
    open('prio')

    const select = await screen.findByRole('combobox', { name: 'Nueva prioridad' })
    await waitFor(() => expect(select).toHaveValue('2')) // Alta
    expect(screen.getByText(/Nuevo vencimiento:/)).toBeInTheDocument()

    const before = screen.getByText(/Nuevo vencimiento:/).textContent
    await userEvent.selectOptions(select, 'Baja · 72 h')
    expect(screen.getByText(/Nuevo vencimiento:/).textContent).not.toBe(before)
  })

  it.each([['Crítica', 1], ['Media', 3], ['Baja', 4]] as [Priority, number][])(
    'cambiar a %s llama a /priority con el id %i',
    async (name, id) => {
      const { calls } = mockWorkflowApi()
      const { onClose } = open('prio')
      const select = await screen.findByRole('combobox', { name: 'Nueva prioridad' })
      await waitFor(() => expect(select).toHaveValue('2'))

      await userEvent.selectOptions(select, screen.getByRole('option', { name: new RegExp(`^${name} ·`) }))
      await submit('Cambiar prioridad')

      await waitFor(() => expect(onClose).toHaveBeenCalled())
      expect(calls).toEqual([{ path: 'SOL-2026-000001/priority', body: { priorityId: id } }])
    },
  )

  it('no pide técnicos', async () => {
    const { technicianAreas } = mockWorkflowApi()
    open('prio')
    await screen.findByRole('combobox', { name: 'Nueva prioridad' })
    expect(technicianAreas).toEqual([])
  })
})

describe('ActionModal: errores del servidor', () => {
  it('un 409 avisa "Transición no permitida", refresca las solicitudes y cierra el diálogo', async () => {
    mockWorkflowApi({ transitions: () => problem(409, 'Conflict', { detail: 'Transición no permitida: de EN_EVALUACION a EN_EVALUACION' }) })
    const { onClose, client } = open('eval')
    const invalidate = vi.spyOn(client, 'invalidateQueries')

    await submit('Pasar a evaluación')

    expect(await screen.findByText('Transición no permitida')).toBeInTheDocument()
    await waitFor(() => expect(onClose).toHaveBeenCalledTimes(1))
    expect(invalidate).toHaveBeenCalledWith({ queryKey: ['requests'] })
    // No es un éxito: no se anuncia que el historial se actualizó.
    expect(screen.queryByText(/listo\. Historial/)).not.toBeInTheDocument()
  })

  it('un 400 con errores por campo los muestra junto al campo y deja el diálogo abierto', async () => {
    mockWorkflowApi({
      transitions: () => problem(400, 'Bad Request', { errors: { technicianId: 'El técnico no pertenece al área.' } }),
    })
    const { onClose } = open('assign')
    await screen.findByRole('option', { name: /Teresa Técnica/ })

    await userEvent.selectOptions(screen.getByRole('combobox', { name: 'Técnico del área' }), 'Teresa Técnica · 0 solicitudes activas')
    await submit('Asignar')

    expect(await screen.findByText('El técnico no pertenece al área.')).toBeInTheDocument()
    expect(onClose).not.toHaveBeenCalled()
    expect(screen.getByRole('dialog', { name: 'Asignar técnico' })).toBeInTheDocument()
  })

  it('un 403 muestra el detalle del servidor y deja el diálogo abierto para reintentar', async () => {
    mockWorkflowApi({ transitions: () => problem(403, 'Forbidden', { detail: 'No eres el técnico asignado.' }) })
    const { onClose } = open('start', ASIGNADA)

    await submit('Iniciar atención')

    expect(await screen.findByText('No eres el técnico asignado.')).toBeInTheDocument()
    expect(onClose).not.toHaveBeenCalled()
    expect(within(screen.getByRole('dialog')).getByRole('button', { name: 'Iniciar atención' })).toBeEnabled()
  })

  it('un error sin detalle usa el título del problema', async () => {
    mockWorkflowApi({ transitions: () => problem(500, 'Error interno') })
    open('eval')

    await submit('Pasar a evaluación')

    expect(await screen.findByText('Error interno')).toBeInTheDocument()
  })
})
