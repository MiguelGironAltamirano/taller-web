import { describe, expect, it } from 'vitest'
import { suggestTriage, type TriageRule } from './suggest'

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
