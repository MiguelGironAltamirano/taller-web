import { describe, expect, it } from 'vitest'
import { stateToStatus, statusToState, toRequestDetail } from './mappers'
import type { RequestDetail } from './types'

/** Detalle tal como lo devuelve hoy el backend: sin lo que aportarán workflow, collaboration y satisfaction. */
const backendDetail: RequestDetail = {
  code: 'SOL-2026-000001',
  categoryName: 'Electricidad',
  locationName: 'Facultad → Pabellón → Aula 101',
  priorityName: 'Alta',
  status: 'REGISTRADA',
  createdAt: '2026-10-17T07:09:43.343159Z',
  dueAt: '2026-10-18T07:09:43.343159Z',
  overdue: false,
  description: 'El tomacorriente del aula no funciona desde ayer',
  requesterName: 'Estudiante Demo',
  areaName: 'Mantenimiento',
  technicianName: null,
  facultyName: 'Facultad',
  pavilionName: 'Pabellón',
  roomName: 'Aula 101',
  requester: { id: 4, fullName: 'Estudiante Demo' },
  area: { id: 1, name: 'Mantenimiento' },
  technician: null,
  closureReason: null,
  history: [
    {
      fromStatus: null,
      toStatus: 'REGISTRADA',
      actorName: 'Estudiante Demo',
      reason: 'Solicitud creada',
      createdAt: '2026-10-17T07:09:43.343159Z',
    },
  ],
  allowedTransitions: [],
}

describe('toRequestDetail', () => {
  it('tolera los campos que todavía no envía el backend', () => {
    const view = toRequestDetail(backendDetail)

    expect(view.comments).toEqual([])
    expect(view.evidences).toEqual([])
    expect(view.allowedActions).toEqual([])
    expect(view.solution).toBeNull()
    expect(view.rating).toBeNull()
  })

  it('traduce estado, fechas y el primer historial', () => {
    const view = toRequestDetail(backendDetail)

    expect(view.state).toBe('Registrada')
    expect(view.createdAt).toBe(Date.parse('2026-10-17T07:09:43.343159Z'))
    expect(view.dueAt - view.createdAt).toBe(24 * 3_600_000)
    expect(view.history).toEqual([
      { from: '—', to: 'Registrada', reason: 'Solicitud creada', who: 'Estudiante Demo', at: view.createdAt },
    ])
    expect(view.areaId).toBe(1)
    expect(view.technicianId).toBeNull()
  })
})

describe('estados', () => {
  it('convierte entre el código del contrato y la etiqueta de la interfaz', () => {
    expect(statusToState('EN_ATENCION')).toBe('En atención')
    expect(stateToStatus('En evaluación')).toBe('EN_EVALUACION')
  })
})
