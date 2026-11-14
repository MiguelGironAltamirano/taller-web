import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import type { WorkflowActionId } from '../../shared/domain'
import { ActionsPanel } from './ActionsPanel'
import { ACTION_CONFIG } from './lib/transitions'
import { detail, mockWorkflowApi, renderWithProviders } from './test-support'

const titles = (actions: WorkflowActionId[]) => actions.map((a) => ACTION_CONFIG[a].title)

describe('ActionsPanel: solo muestra lo que el servidor permite', () => {
  it('no dibuja nada si no hay acciones permitidas', () => {
    mockWorkflowApi()
    const { container } = renderWithProviders(<ActionsPanel request={detail({ allowedActions: [] })} />)
    expect(container).toBeEmptyDOMElement()
  })

  it('muestra un botón por cada acción permitida y ninguno más', () => {
    mockWorkflowApi()
    const allowed: WorkflowActionId[] = ['eval', 'noproc', 'dup']
    renderWithProviders(<ActionsPanel request={detail({ allowedActions: allowed })} />)

    expect(screen.getByText('Acciones')).toBeInTheDocument()
    expect(screen.getAllByRole('button').map((b) => b.textContent)).toEqual(titles(allowed))
    expect(screen.queryByRole('button', { name: ACTION_CONFIG.assign.title })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: ACTION_CONFIG.resolve.title })).not.toBeInTheDocument()
  })

  it('respeta el orden que envía el servidor', () => {
    mockWorkflowApi()
    const allowed: WorkflowActionId[] = ['start', 'reassign']
    renderWithProviders(<ActionsPanel request={detail({ state: 'Asignada', allowedActions: allowed })} />)
    expect(screen.getAllByRole('button').map((b) => b.textContent)).toEqual(titles(allowed))
  })

  it('no abre ningún diálogo hasta que se pulsa un botón', () => {
    mockWorkflowApi()
    renderWithProviders(<ActionsPanel request={detail({ allowedActions: ['eval'] })} />)
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
  })
})

describe('ActionsPanel: abre el diálogo de la acción elegida', () => {
  it('abre el diálogo con el título de la acción y el código de la solicitud', async () => {
    mockWorkflowApi()
    renderWithProviders(<ActionsPanel request={detail({ allowedActions: ['eval', 'noproc'] })} />)

    await userEvent.click(screen.getByRole('button', { name: 'Cerrar como No procede' }))

    const dialog = await screen.findByRole('dialog', { name: 'Cerrar como No procede' })
    expect(within(dialog).getByText('SOL-2026-000001')).toBeInTheDocument()
  })

  it('"Cancelar" cierra el diálogo sin llamar a la API', async () => {
    const { calls } = mockWorkflowApi()
    renderWithProviders(<ActionsPanel request={detail({ allowedActions: ['eval'] })} />)

    await userEvent.click(screen.getByRole('button', { name: 'Pasar a evaluación' }))
    await userEvent.click(await within(await screen.findByRole('dialog')).findByRole('button', { name: 'Cancelar' }))

    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    expect(calls).toEqual([])
  })

  it('Escape también cierra el diálogo', async () => {
    mockWorkflowApi()
    renderWithProviders(<ActionsPanel request={detail({ allowedActions: ['start'], state: 'Asignada' })} />)

    await userEvent.click(screen.getByRole('button', { name: 'Iniciar atención' }))
    await screen.findByRole('dialog', { name: 'Iniciar atención' })
    await userEvent.keyboard('{Escape}')

    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
  })
})
