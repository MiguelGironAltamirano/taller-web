import { describe, expect, it } from 'vitest'
import type { RequestRow, RequestState } from '../../../shared/domain'
import { attentionFor, sortByUrgency } from './attention'
import { withSla } from './view'

const NOW = Date.parse('2026-11-14T10:00:00-05:00')
const H = 3_600_000

const base: RequestRow = {
  code: 'X',
  requester: 'Ana',
  category: 'Electricidad',
  area: 'Mantenimiento',
  faculty: 'f',
  pavilion: 'p',
  room: 'r',
  description: 'd',
  priority: 'Media',
  state: 'Registrada',
  technician: null,
  createdAt: NOW - H,
  dueAt: NOW + 10 * H,
}
const row = (state: RequestState, extra: Partial<RequestRow> = {}) => withSla({ ...base, state, ...extra }, NOW)
const none = new Set<string>()

describe('attentionFor', () => {
  it('el supervisor atiende lo nuevo y lo que falta asignar', () => {
    expect(attentionFor('supervisor', row('Registrada'), none)?.cta).toBe('Evaluar')
    expect(attentionFor('supervisor', row('En evaluación'), none)?.cta).toBe('Asignar')
    expect(attentionFor('supervisor', row('En atención'), none)).toBeNull()
  })

  it('el técnico atiende lo que se le asignó y el solicitante confirma la solución', () => {
    expect(attentionFor('tecnico', row('Asignada'), none)?.cta).toBe('Atender')
    expect(attentionFor('solicitante', row('Resuelta'), none)?.cta).toBe('Revisar')
    expect(attentionFor('solicitante', row('Registrada'), none)).toBeNull()
  })

  it('una notificación sin leer marca novedades, salvo en solicitudes finalizadas', () => {
    const unseen = new Set(['X'])
    expect(attentionFor('tecnico', row('En atención'), unseen)?.reason).toBe('Tiene novedades')
    expect(attentionFor('tecnico', row('Cerrada'), unseen)).toBeNull()
  })
})

describe('sortByUrgency', () => {
  it('ordena por novedad, luego vencidas, prioridad y vencimiento', () => {
    const rows = [
      row('Registrada', { code: 'baja', priority: 'Baja' }),
      row('Registrada', { code: 'critica', priority: 'Crítica' }),
      row('Registrada', { code: 'vencida', dueAt: NOW - H }),
      row('Registrada', { code: 'nueva', priority: 'Baja' }),
    ]
    expect(sortByUrgency(rows, new Set(['nueva'])).map((r) => r.code)).toEqual(['nueva', 'vencida', 'critica', 'baja'])
  })
})
