import { describe, expect, it } from 'vitest'
import type { RequestRow } from '../../../shared/domain'
import type { TeamMemberLoad } from '../../../shared/api/types'
import { withSla } from '../../requests'
import { REPORTS, buildReport, kpiTone } from './reports'

const H = 3_600_000
const NOW = new Date('2026-10-16T10:00:00-05:00').getTime()
const base: RequestRow = {
  code: 'A',
  requester: 'x',
  category: 'Electricidad',
  area: 'Mantenimiento e Infraestructura',
  faculty: 'f',
  pavilion: 'p',
  room: 'r',
  description: 'd',
  priority: 'Alta',
  state: 'En atención',
  technician: 'Carlos',
  createdAt: NOW - 10 * H,
  dueAt: NOW + 5 * H,
}
const rows = [
  withSla({ ...base, code: 'A' }, NOW),
  withSla({ ...base, code: 'B', dueAt: NOW - 3 * H }, NOW), // vencida
  withSla({ ...base, code: 'C', state: 'Resuelta', dueAt: NOW - 30 * H, priority: 'Baja' }, NOW),
]
const team: TeamMemberLoad[] = [{ id: 1, fullName: 'Carlos', assigned: 3, inProgress: 2, overdue: 1, capacity: 6 }]

describe('buildReport', () => {
  it('"Solicitudes por estado" cuenta cada estado', () => {
    const { csv, filename } = buildReport('estado', rows, team)
    expect(filename).toMatch(/^solicitudes-por-estado/)
    expect(csv).toContain('En atención,2')
    expect(csv).toContain('Resuelta,1')
    expect(csv).toContain('Cerrada,0')
  })

  it('"Cumplimiento de SLA" separa dentro y fuera de plazo por prioridad', () => {
    const { csv } = buildReport('sla', rows, team)
    expect(csv.split('\n')[0]).toBe('Prioridad,Total,Dentro de plazo,Fuera de plazo,Cumplimiento (%)')
    expect(csv).toContain('Alta,2,1,1,50')
    expect(csv).toContain('Baja,1,1,0,100')
  })

  it('"Carga por técnico" usa los datos del equipo', () => {
    expect(buildReport('carga', rows, team).csv).toContain('Carlos,3,2,1,6')
  })

  it('"Solicitudes vencidas" lista solo las vencidas con su retraso', () => {
    const { csv } = buildReport('vencidas', rows, team)
    expect(csv).toContain('B,')
    expect(csv).not.toContain('\nA,')
    expect(csv).toContain('Vencida hace 3 h')
  })

  it('define los cuatro reportes del diseño', () => {
    expect(REPORTS.map((r) => r.id)).toEqual(['estado', 'sla', 'carga', 'vencidas'])
  })
})

describe('kpiTone', () => {
  it('sube bien cuando más es mejor y mal cuando es peor', () => {
    expect(kpiTone(12, true)).toBe('good')
    expect(kpiTone(-8, true)).toBe('bad')
    expect(kpiTone(-8, false)).toBe('good')
    expect(kpiTone(10, false)).toBe('bad')
  })
})
