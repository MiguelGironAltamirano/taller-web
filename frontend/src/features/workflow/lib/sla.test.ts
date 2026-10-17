import { describe, expect, it } from 'vitest'
import { computeDueAt, formatDuration, slaStatus } from './sla'

const H = 3_600_000
const T0 = new Date('2026-10-16T10:00:00-05:00').getTime()

describe('computeDueAt', () => {
  it('suma las horas de SLA de la prioridad a la fecha de registro', () => {
    expect(computeDueAt(T0, 8)).toBe(T0 + 8 * H)
    expect(computeDueAt(T0, 120)).toBe(T0 + 120 * H)
  })
})

describe('slaStatus', () => {
  const created = T0 - 10 * H
  const due = created + 24 * H

  it('marca como vencida una solicitud sin resolver pasada su fecha', () => {
    const s = slaStatus({ state: 'En atención', createdAt: created, dueAt: T0 - 4 * H }, T0)
    expect(s.overdue).toBe(true)
    expect(s.label).toBe('Vencida hace 4 h')
  })

  it('no marca como vencida una solicitud resuelta a tiempo', () => {
    const s = slaStatus({ state: 'Resuelta', createdAt: created, dueAt: due }, T0)
    expect(s.overdue).toBe(false)
    expect(s.label).toBe('Atendida')
    expect(s.pct).toBe(100)
  })

  it('no marca como vencida una solicitud cerrada aunque pasara el plazo', () => {
    const s = slaStatus({ state: 'Cerrada', createdAt: created, dueAt: T0 - H }, T0)
    expect(s.overdue).toBe(false)
    expect(s.label).toBe('Cerrada')
  })

  it('calcula el porcentaje consumido y el tiempo restante', () => {
    const s = slaStatus({ state: 'Asignada', createdAt: created, dueAt: due }, T0)
    expect(s.pct).toBeCloseTo((10 / 24) * 100)
    expect(s.label).toBe('Vence en 14 h')
    expect(s.tone).toBe('ok')
  })

  it('pasa a tono de advertencia al superar el 80 %', () => {
    const s = slaStatus({ state: 'Asignada', createdAt: T0 - 21 * H, dueAt: T0 + 3 * H }, T0)
    expect(s.tone).toBe('warn')
  })
})

describe('formatDuration', () => {
  it('usa horas por debajo de 48 h y días a partir de ahí', () => {
    expect(formatDuration(5 * H)).toBe('5 h')
    expect(formatDuration(47 * H)).toBe('47 h')
    expect(formatDuration(72 * H)).toBe('3 d')
  })
})
