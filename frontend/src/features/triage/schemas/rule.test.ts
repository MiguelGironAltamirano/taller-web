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
  it('limita las palabras clave a 20, cada una de 2 a 50 caracteres', () => {
    const many = Array.from({ length: 21 }, (_, i) => `palabra${i}`)
    expect(ruleSchema.safeParse({ ...ok, keywords: many.join(',') }).success).toBe(false)
    expect(ruleSchema.safeParse({ ...ok, keywords: many.slice(0, 20).join(',') }).success).toBe(true)
    expect(ruleSchema.safeParse({ ...ok, keywords: 'wifi, x' }).success).toBe(false)
    expect(ruleSchema.safeParse({ ...ok, keywords: 'x'.repeat(51) }).success).toBe(false)
  })
})
