import { describe, expect, it } from 'vitest'
import { dateLong, dateShort, dayGroup, normalizeText } from './format'

const H = 3_600_000
const NOW = new Date('2026-10-16T10:00:00-05:00').getTime()

describe('formato de fechas (zona horaria de Lima)', () => {
  it('dateShort muestra día y mes abreviado', () => {
    expect(dateShort(NOW)).toBe('16 oct')
  })
  it('dateLong incluye la hora en formato 24 h', () => {
    expect(dateLong(NOW)).toMatch(/16 oct,? 10:00/)
  })
})

describe('dayGroup', () => {
  it('agrupa por antigüedad', () => {
    expect(dayGroup(NOW - 2 * H, NOW)).toBe('Hoy')
    expect(dayGroup(NOW - 30 * H, NOW)).toBe('Ayer')
    expect(dayGroup(NOW - 120 * H, NOW)).toBe('Anteriores')
  })

  it('usa días calendario de Lima, no bloques de 24 horas', () => {
    const earlyMorning = new Date('2026-10-16T03:00:00-05:00').getTime()
    expect(dayGroup(new Date('2026-10-16T00:30:00-05:00').getTime(), earlyMorning)).toBe('Hoy')
    expect(dayGroup(new Date('2026-10-15T23:00:00-05:00').getTime(), earlyMorning)).toBe('Ayer')
    expect(dayGroup(new Date('2026-10-15T00:10:00-05:00').getTime(), earlyMorning)).toBe('Ayer')
    expect(dayGroup(new Date('2026-10-14T23:59:00-05:00').getTime(), earlyMorning)).toBe('Anteriores')
  })
})

describe('normalizeText', () => {
  it('quita tildes y pasa a minúsculas', () => {
    expect(normalizeText('Instalación ELÉCTRICA')).toBe('instalacion electrica')
  })
})
