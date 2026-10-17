import { describe, expect, it } from 'vitest'
import type { RequestRow } from '../../../shared/domain'
import { countByState, filterRows, sortRows, toCsv, type ListFilters } from './filters'
import { withSla, type RowView } from './view'

const H = 3_600_000
const NOW = new Date('2026-10-16T10:00:00-05:00').getTime()

const base: RequestRow = {
  code: 'SOL-2026-000001',
  requester: 'Lucía Quispe Huamán',
  category: 'Electricidad',
  area: 'Mantenimiento e Infraestructura',
  faculty: 'Facultad de Medicina',
  pavilion: 'Pabellón Central',
  room: 'Aula 12',
  description: 'Fuga de agua en el techo',
  priority: 'Media',
  state: 'En atención',
  technician: 'Carlos Mendoza Rojas',
  createdAt: NOW - 10 * H,
  dueAt: NOW + 20 * H,
}
const row = (over: Partial<RequestRow>): RowView => withSla({ ...base, ...over }, NOW)

const none: ListFilters = { q: '', category: '', priority: '', technician: '', status: '', view: 'all', sort: 'due' }

const rows: RowView[] = [
  row({ code: 'A', state: 'Registrada', technician: null, priority: 'Crítica', dueAt: NOW + 2 * H }),
  row({ code: 'B', state: 'En atención', dueAt: NOW - 5 * H, priority: 'Alta' }), // vencida
  row({ code: 'C', state: 'Resuelta', dueAt: NOW - 50 * H, priority: 'Baja' }), // resuelta: no vencida
  row({ code: 'D', state: 'Cerrada', category: 'Red e internet', description: 'Sin wifi', priority: 'Media' }),
  row({ code: 'E', state: 'Asignada', technician: 'Rosa Alarcón Díaz', createdAt: NOW - 1 * H, dueAt: NOW + 70 * H }),
]
const codes = (rs: RowView[]) => rs.map((r) => r.code)

describe('filterRows', () => {
  it('sin filtros devuelve todo', () => {
    expect(filterRows(rows, none)).toHaveLength(5)
  })
  it('busca por código, descripción, categoría y ambiente sin importar tildes', () => {
    expect(codes(filterRows(rows, { ...none, q: 'wifi' }))).toEqual(['D'])
    expect(codes(filterRows(rows, { ...none, q: 'aula 12' }))).toEqual(['A', 'B', 'C', 'D', 'E'])
    expect(codes(filterRows(rows, { ...none, q: 'ELECTRICIDAD' }))).toEqual(['A', 'B', 'C', 'E'])
  })
  it('filtra por categoría, prioridad y técnico', () => {
    expect(codes(filterRows(rows, { ...none, category: 'Red e internet' }))).toEqual(['D'])
    expect(codes(filterRows(rows, { ...none, priority: 'Crítica' }))).toEqual(['A'])
    expect(codes(filterRows(rows, { ...none, technician: 'Rosa Alarcón Díaz' }))).toEqual(['E'])
  })
  it('filtra por estado, incluida la opción "vencidas"', () => {
    expect(codes(filterRows(rows, { ...none, status: 'Resuelta' }))).toEqual(['C'])
    expect(codes(filterRows(rows, { ...none, status: '__venc' }))).toEqual(['B'])
  })
  it('vista "Mis vencidas": solo sin resolver y pasadas de plazo', () => {
    expect(codes(filterRows(rows, { ...none, view: 'venc' }))).toEqual(['B'])
  })
  it('vista "Abiertas": excluye resueltas y cerradas', () => {
    expect(codes(filterRows(rows, { ...none, view: 'open' }))).toEqual(['A', 'B', 'E'])
  })
  it('vista "Por confirmar": solo resueltas', () => {
    expect(codes(filterRows(rows, { ...none, view: 'confirm' }))).toEqual(['C'])
  })
  it('vista "Críticas sin asignar"', () => {
    expect(codes(filterRows(rows, { ...none, view: 'critsa' }))).toEqual(['A'])
  })
})

describe('sortRows', () => {
  it('por vencimiento: primero las abiertas más urgentes y al final las finalizadas', () => {
    expect(codes(sortRows(rows, 'due'))).toEqual(['B', 'A', 'E', 'C', 'D'])
  })
  it('por más recientes', () => {
    expect(codes(sortRows(rows, 'new'))[0]).toBe('E')
  })
  it('por prioridad', () => {
    expect(codes(sortRows(rows, 'prio')).slice(0, 2)).toEqual(['A', 'B'])
  })
  it('no modifica el arreglo original', () => {
    const before = codes(rows)
    sortRows(rows, 'prio')
    expect(codes(rows)).toEqual(before)
  })
})

describe('countByState', () => {
  it('cuenta por estado y vencidas', () => {
    const c = countByState(rows)
    expect(c.total).toBe(5)
    expect(c.overdue).toBe(1)
    expect(c.byState['Resuelta']).toBe(1)
    expect(c.byState['En atención']).toBe(1)
  })
})

describe('toCsv', () => {
  it('genera cabecera y escapa comillas y comas', () => {
    const csv = toCsv([row({ code: 'X', description: 'Dice "hola", y luego sigue' })])
    const [header, line] = csv.split('\n')
    expect(header.startsWith('Código,Categoría,Ubicación')).toBe(true)
    expect(line.startsWith('X,')).toBe(true)
    expect(csv).toContain('Facultad de Medicina → Pabellón Central → Aula 12')
    expect(csv).toContain('"Dice ""hola"", y luego sigue"')
  })
})
