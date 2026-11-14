import { fireEvent, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import type { Role } from '../../shared/domain'
import { KanbanBoard, type KanbanItem } from './KanbanBoard'
import { mockWorkflowApi, renderWithProviders, row } from './test-support'

const SLA: KanbanItem['sla'] = { overdue: false, pct: 40, label: 'Faltan 3 h', tone: 'ok' }

const item = (overrides: Partial<KanbanItem> = {}): KanbanItem => ({ ...row(), sla: SLA, ...overrides })

function renderBoard(items: KanbanItem[], role: Role = 'supervisor') {
  const onOpen = vi.fn()
  const view = renderWithProviders(<KanbanBoard items={items} role={role} onOpen={onOpen} />)
  return { onOpen, ...view }
}

const column = (state: string, count: number) => screen.getByRole('group', { name: `${state}: ${count}` })

/** Arrastra la tarjeta a la columna, como lo hace el navegador (dragstart → dragover → drop). */
function drag(code: string, toColumn: HTMLElement) {
  const dataTransfer = { setData: vi.fn(), getData: () => code }
  fireEvent.dragStart(screen.getByRole('button', { name: new RegExp(code) }), { dataTransfer })
  fireEvent.dragOver(toColumn, { dataTransfer })
  fireEvent.drop(toColumn, { dataTransfer })
}

describe('KanbanBoard: columnas y tarjetas', () => {
  it('reparte las solicitudes por estado y cuenta cada columna', () => {
    mockWorkflowApi()
    renderBoard([
      item({ code: 'SOL-1', state: 'Registrada' }),
      item({ code: 'SOL-2', state: 'Registrada' }),
      item({ code: 'SOL-3', state: 'Asignada' }),
    ])

    expect(within(column('Registrada', 2)).getByText('SOL-1')).toBeInTheDocument()
    expect(within(column('Registrada', 2)).getByText('SOL-2')).toBeInTheDocument()
    expect(within(column('Asignada', 1)).getByText('SOL-3')).toBeInTheDocument()
    expect(column('En evaluación', 0)).toBeInTheDocument()
    expect(column('Cerrada', 0)).toBeInTheDocument()
  })

  it('la tarjeta muestra categoría, ambiente, prioridad y tiempo restante', () => {
    mockWorkflowApi()
    renderBoard([item()])

    const card = screen.getByRole('button', { name: /SOL-2026-000001/ })
    expect(within(card).getByText('Electricidad')).toBeInTheDocument()
    expect(within(card).getByText('Aula 101')).toBeInTheDocument()
    expect(within(card).getByText('Alta')).toBeInTheDocument()
    expect(within(card).getByText('Faltan 3 h')).toBeInTheDocument()
  })

  it('marca como vencidas las solicitudes fuera de plazo', () => {
    mockWorkflowApi()
    renderBoard([item({ sla: { ...SLA, overdue: true, pct: 100, tone: 'bad', label: 'Venció hace 2 h' } })])
    expect(screen.getByText('Venció hace 2 h')).toBeInTheDocument()
    expect(screen.getByText('Vencida')).toBeInTheDocument()
  })

  it('abrir una tarjeta con clic o con Enter avisa con su código', async () => {
    mockWorkflowApi()
    const { onOpen } = renderBoard([item()])
    const card = screen.getByRole('button', { name: /SOL-2026-000001/ })

    await userEvent.click(card)
    card.focus()
    await userEvent.keyboard('{Enter}')

    expect(onOpen).toHaveBeenNthCalledWith(1, 'SOL-2026-000001')
    expect(onOpen).toHaveBeenNthCalledWith(2, 'SOL-2026-000001')
  })
})

describe('KanbanBoard: arrastrar una tarjeta', () => {
  it('una transición válida del supervisor abre el diálogo de la acción', async () => {
    mockWorkflowApi()
    renderBoard([item({ state: 'Registrada' })])

    drag('SOL-2026-000001', column('En evaluación', 0))

    expect(await screen.findByRole('dialog', { name: 'Pasar a evaluación' })).toBeInTheDocument()
  })

  it('de En evaluación a Asignada pide elegir técnico', async () => {
    mockWorkflowApi()
    renderBoard([item({ state: 'En evaluación' })])

    drag('SOL-2026-000001', column('Asignada', 0))

    expect(await screen.findByRole('dialog', { name: 'Asignar técnico' })).toBeInTheDocument()
    expect(await screen.findByRole('combobox', { name: 'Técnico del área' })).toBeInTheDocument()
  })

  it.each(['Registrada', 'En evaluación'] as const)('de %s a Cerrada abre "Cerrar como No procede"', async (state) => {
    mockWorkflowApi()
    renderBoard([item({ state })])

    drag('SOL-2026-000001', column('Cerrada', 0))

    expect(await screen.findByRole('dialog', { name: 'Cerrar como No procede' })).toBeInTheDocument()
  })

  it('el técnico mueve Asignada → En atención y En atención → Resuelta', async () => {
    mockWorkflowApi()
    const { unmount } = renderBoard([item({ state: 'Asignada', technician: 'Tomás Técnico' })], 'tecnico')
    drag('SOL-2026-000001', column('En atención', 0))
    expect(await screen.findByRole('dialog', { name: 'Iniciar atención' })).toBeInTheDocument()
    unmount()

    renderBoard([item({ state: 'En atención', technician: 'Tomás Técnico' })], 'tecnico')
    drag('SOL-2026-000001', column('Resuelta', 0))
    expect(await screen.findByRole('dialog', { name: 'Marcar como resuelta' })).toBeInTheDocument()
  })

  it.each([
    ['supervisor', 'Registrada', 'Resuelta'],
    ['supervisor', 'Registrada', 'Asignada'],
    ['supervisor', 'Asignada', 'En atención'],
    ['supervisor', 'En atención', 'Cerrada'],
    ['supervisor', 'Resuelta', 'Cerrada'],
    ['tecnico', 'Registrada', 'En evaluación'],
    ['tecnico', 'Asignada', 'Resuelta'],
    ['tecnico', 'En atención', 'Cerrada'],
    ['solicitante', 'Registrada', 'En evaluación'],
    ['administrador', 'Asignada', 'En atención'],
  ] as const)('%s no puede mover %s → %s: avisa y no llama a la API', async (role, from, to) => {
    const { calls } = mockWorkflowApi()
    renderBoard([item({ state: from })], role)

    drag('SOL-2026-000001', column(to, 0))

    expect(await screen.findByText(`Transición no válida: ${from} → ${to}`)).toBeInTheDocument()
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    expect(calls).toEqual([])
  })

  it('soltar la tarjeta en su misma columna no hace nada', () => {
    const { calls } = mockWorkflowApi()
    renderBoard([item({ state: 'Registrada' })])

    drag('SOL-2026-000001', column('Registrada', 1))

    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    expect(screen.queryByText(/Transición no válida/)).not.toBeInTheDocument()
    expect(calls).toEqual([])
  })

  it('soltar sin haber arrastrado una tarjeta no hace nada', () => {
    mockWorkflowApi()
    renderBoard([item()])

    fireEvent.drop(column('En evaluación', 0), { dataTransfer: { getData: () => '' } })

    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    expect(screen.queryByText(/Transición no válida/)).not.toBeInTheDocument()
  })

  it('confirmar la acción del diálogo ejecuta la transición sobre la tarjeta arrastrada', async () => {
    const { calls } = mockWorkflowApi()
    renderBoard([item({ code: 'SOL-1' }), item({ code: 'SOL-2' })])

    drag('SOL-2', column('En evaluación', 0))
    await userEvent.click(await screen.findByRole('button', { name: 'Pasar a evaluación' }))

    await screen.findByText('Pasar a evaluación: listo. Historial y notificación actualizados.')
    expect(calls).toEqual([{ path: 'SOL-2/transitions', body: { to: 'EN_EVALUACION' } }])
  })

  it('cancelar el diálogo cierra y no cambia nada', async () => {
    const { calls } = mockWorkflowApi()
    renderBoard([item()])

    drag('SOL-2026-000001', column('En evaluación', 0))
    await userEvent.click(await screen.findByRole('button', { name: 'Cancelar' }))

    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    expect(calls).toEqual([])
  })
})
