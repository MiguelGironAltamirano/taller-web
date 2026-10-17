import { describe, expect, it } from 'vitest'
import { parseKeywords, ruleSchema } from './rule'

describe('parseKeywords', () => {
  it('separa por comas, recorta y descarta vacíos', () => {
    expect(parseKeywords(' chispa, humo ,, incendio ')).toEqual(['chispa', 'humo', 'incendio'])
    expect(parseKeywords('   ')).toEqual([])
  })
})

describe('ruleSchema', () => {
  const ok = { keywords: 'wifi, internet', categoryId: '4', priorityId: '2' }
  it('acepta una regla completa', () => {
    expect(ruleSchema.safeParse(ok).success).toBe(true)
  })
  it('exige al menos una palabra clave, categoría y prioridad', () => {
    expect(ruleSchema.safeParse({ ...ok, keywords: ' , ' }).success).toBe(false)
    expect(ruleSchema.safeParse({ ...ok, categoryId: '' }).success).toBe(false)
    expect(ruleSchema.safeParse({ ...ok, priorityId: '' }).success).toBe(false)
  })
})
