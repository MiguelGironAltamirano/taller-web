import { describe, expect, it } from 'vitest'
import { matchTriage, suggestTriage, type TriageRule } from './suggest'

const rules: TriageRule[] = [
  { id: 1, keywords: ['chispa', 'humo'], category: 'Electricidad', priority: 'Crítica', active: true },
  { id: 2, keywords: ['luz', 'foco', 'tablero'], category: 'Electricidad', priority: 'Alta', active: true },
  { id: 3, keywords: ['wifi', 'internet'], category: 'Red e internet', priority: 'Alta', active: true },
  { id: 4, keywords: ['proyector'], category: 'Equipos de cómputo', priority: 'Media', active: false },
]

describe('suggestTriage', () => {
  it('sugiere la regla con más coincidencias', () => {
    const r = suggestTriage('Salen chispas del tablero y huele a humo', rules)
    expect(r).toEqual({ category: 'Electricidad', priority: 'Crítica' })
  })
  it('ignora mayúsculas y tildes', () => {
    expect(suggestTriage('SIN INTERNET en la sala', rules)).toEqual({ category: 'Red e internet', priority: 'Alta' })
  })
  it('no sugiere nada sin coincidencias', () => {
    expect(suggestTriage('Necesito una constancia', rules)).toBeNull()
  })
  it('no usa reglas inactivas', () => {
    expect(suggestTriage('El proyector no enciende', rules)).toBeNull()
  })
  it('no sugiere con descripciones muy cortas', () => {
    expect(suggestTriage('luz', rules)).toBeNull()
  })
})

describe('matchTriage', () => {
  it('solo cuenta palabras o frases completas', () => {
    expect(matchTriage('Las luces del pasillo parpadean', rules)).toBeNull()
    expect(matchTriage('El foco, la luz; todo falla', rules)?.id).toBe(2)
    const phrase = [{ id: 9, keywords: ['aula virtual'], active: true }]
    expect(matchTriage('No entro al AULA   virtual', phrase)?.id).toBe(9)
    expect(matchTriage('El aula es virtual', phrase)).toBeNull()
  })
  it('en empate gana la regla de menor id aunque llegue después', () => {
    const tied = [
      { id: 7, keywords: ['wifi'], active: true },
      { id: 3, keywords: ['internet'], active: true },
    ]
    expect(matchTriage('Sin wifi ni internet', tied)?.id).toBe(3)
  })
  it('cada palabra clave cuenta una vez', () => {
    const r = [
      { id: 1, keywords: ['humo', 'HÚMO'], active: true },
      { id: 2, keywords: ['humo', 'chispa'], active: true },
    ]
    expect(matchTriage('humo y chispa', r)?.id).toBe(2)
  })
})
