import { describe, expect, it } from 'vitest'
import type { RequestRow } from '../../../shared/domain'
import { buildMonthGrid, type CalendarCell } from './calendar'
import { withSla } from './view'

const H = 3_600_000
const NOW = new Date('2026-10-16T10:00:00-05:00').getTime()
const lima = (iso: string) => new Date(`${iso}-05:00`).getTime()

const base: RequestRow = {
  code: 'X',
  requester: 'a',
  category: 'Electricidad',
  area: 'Mantenimiento e Infraestructura',
  faculty: 'f',
  pavilion: 'p',
  room: 'r',
  description: 'd',
  priority: 'Media',
  state: 'En atención',
  technician: 'Carlos',
  createdAt: NOW - H,
  dueAt: NOW + H,
}
const row = (code: string, dueAt: number, state: RequestRow['state'] = 'En atención') =>
  withSla({ ...base, code, dueAt, state }, NOW)

describe('buildMonthGrid', () => {
  it('octubre de 2026 empieza en jueves: 3 celdas vacías y completa semanas de 7', () => {
    const grid = buildMonthGrid([], NOW)
    expect(grid.title).toBe('Octubre 2026')
    expect(grid.cells.slice(0, 3).every((c) => c.blank)).toBe(true)
    expect(grid.cells[3]).toMatchObject({ blank: false, day: 1 })
    expect(grid.cells.length % 7).toBe(0)
    expect(grid.cells.filter((c) => !c.blank)).toHaveLength(31)
  })

  it('marca el día de hoy', () => {
    const today = buildMonthGrid([], NOW).cells.filter(
      (c): c is Extract<CalendarCell, { blank: false }> => !c.blank && c.today,
    )
    expect(today.map((c) => c.day)).toEqual([16])
  })

  it('ubica cada solicitud en el día de su vencimiento (hora de Lima)', () => {
    const grid = buildMonthGrid(
      [row('A', lima('2026-10-20T09:00:00')), row('B', lima('2026-10-20T23:30:00')), row('C', lima('2026-10-05T08:00:00'))],
      NOW,
    )
    const day = (n: number) =>
      grid.cells.find((c): c is Extract<CalendarCell, { blank: false }> => !c.blank && c.day === n)!
    expect(day(20).items.map((i) => i.code)).toEqual(['A', 'B'])
    expect(day(5).items.map((i) => i.code)).toEqual(['C'])
  })

  it('ignora vencimientos de otros meses', () => {
    const grid = buildMonthGrid([row('N', lima('2026-11-03T10:00:00'))], NOW)
    expect(grid.cells.flatMap((c) => c.items)).toHaveLength(0)
  })
})
