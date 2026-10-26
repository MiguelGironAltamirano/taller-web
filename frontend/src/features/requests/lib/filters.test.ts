import { describe, expect, it } from 'vitest'
import type { RequestRow } from '../../../shared/domain'
import { openCount, toCsv, toServerQuery, toStripCounts, type ListFilters } from './filters'
import { withSla, type RowView } from './view'

const H = 3_600_000
const NOW = new Date('2026-10-16T10:00:00-05:00').getTime()
const CRITICAL = 1

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
const plan = (f: Partial<ListFilters>) => toServerQuery({ ...none, ...f }, CRITICAL)
const OPEN = ['REGISTRADA', 'EN_EVALUACION', 'ASIGNADA', 'EN_ATENCION']

describe('toServerQuery', () => {
  it('sin filtros solo envía el orden', () => {
    expect(plan({})).toEqual({ kind: 'query', query: { sort: 'due' } })
    expect(plan({ sort: 'new' })).toEqual({ kind: 'query', query: { sort: 'newest' } })
    expect(plan({ sort: 'prio' })).toEqual({ kind: 'query', query: { sort: 'priority' } })
  })

  it('envía texto recortado e ids numéricos de categoría, prioridad y técnico', () => {
    expect(plan({ q: '  aula 12 ', category: '6', priority: '2', technician: '9' })).toEqual({
      kind: 'query',
      query: { sort: 'due', q: 'aula 12', categoryId: 6, priorityId: 2, technicianId: 9 },
    })
    expect(plan({ category: 'Electricidad' })).toEqual({ kind: 'query', query: { sort: 'due' } })
  })

  it('traduce el estado de la interfaz al código del contrato y "Vencidas" a overdue', () => {
    expect(plan({ status: 'En atención' })).toEqual({ kind: 'query', query: { sort: 'due', status: ['EN_ATENCION'] } })
    expect(plan({ status: '__venc' })).toEqual({ kind: 'query', query: { sort: 'due', overdue: true } })
  })

  it('traduce las vistas guardadas', () => {
    expect(plan({ view: 'venc' })).toEqual({ kind: 'query', query: { sort: 'due', overdue: true } })
    expect(plan({ view: 'open' })).toEqual({ kind: 'query', query: { sort: 'due', status: OPEN } })
    expect(plan({ view: 'confirm' })).toEqual({ kind: 'query', query: { sort: 'due', status: ['RESUELTA'] } })
    expect(plan({ view: 'critsa' })).toEqual({
      kind: 'query',
      query: { sort: 'due', priorityId: CRITICAL, unassigned: true, status: OPEN },
    })
  })

  it('intersecta el estado con la vista', () => {
    expect(plan({ status: 'Asignada', view: 'open' })).toEqual({ kind: 'query', query: { sort: 'due', status: ['ASIGNADA'] } })
    expect(plan({ status: 'Resuelta', view: 'open' })).toEqual({ kind: 'empty' })
    expect(plan({ status: 'Cerrada', view: 'critsa' })).toEqual({ kind: 'empty' })
  })

  it('detecta combinaciones imposibles de "Críticas sin asignar"', () => {
    expect(plan({ view: 'critsa', priority: '3' })).toEqual({ kind: 'empty' })
    expect(plan({ view: 'critsa', technician: '9' })).toEqual({ kind: 'empty' })
    expect(plan({ view: 'critsa', priority: String(CRITICAL) }).kind).toBe('query')
  })

  it('espera al catálogo cuando necesita el id de la prioridad crítica', () => {
    expect(toServerQuery({ ...none, view: 'critsa' }, undefined)).toEqual({ kind: 'pending' })
    expect(toServerQuery({ ...none, view: 'open' }, undefined).kind).toBe('query')
  })
})

describe('contadores', () => {
  const counts = {
    total: 9,
    overdue: 2,
    byStatus: { REGISTRADA: 1, EN_EVALUACION: 2, ASIGNADA: 1, EN_ATENCION: 1, RESUELTA: 3, CERRADA: 1 },
  }

  it('usa las etiquetas de la interfaz', () => {
    expect(toStripCounts(counts)).toEqual({
      total: 9,
      overdue: 2,
      byState: { Registrada: 1, 'En evaluación': 2, Asignada: 1, 'En atención': 1, Resuelta: 3, Cerrada: 1 },
    })
    expect(toStripCounts(undefined).total).toBe(0)
  })

  it('las abiertas son las que no llegaron a Resuelta', () => {
    expect(openCount(counts)).toBe(5)
    expect(openCount(undefined)).toBe(0)
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
