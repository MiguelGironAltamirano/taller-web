import { describe, expect, it } from 'vitest'
import { buildCsv } from './csv'

describe('buildCsv', () => {
  it('une cabecera y filas', () => {
    expect(buildCsv(['a', 'b'], [[1, 'x']])).toBe('a,b\n1,x')
  })
  it('escapa comas, comillas y saltos de línea', () => {
    expect(buildCsv(['t'], [['Dice "hola", y\nsigue']])).toBe('t\n"Dice ""hola"", y\nsigue"')
  })
})
