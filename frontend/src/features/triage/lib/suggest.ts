import type { Priority } from '../../../shared/domain'

export type TriageRule = {
  id: number
  keywords: string[]
  category: string
  priority: Priority
  active: boolean
}

export type TriageSuggestion = { category: string; priority: Priority }

const MIN_LENGTH = 8

const normalize = (s: string) => s.toLowerCase().normalize('NFD').replace(/\p{Diacritic}/gu, '')

/**
 * Sugiere categoría y prioridad según la regla con más coincidencias de palabras clave (spec §5.1).
 * Sin coincidencias no hay sugerencia. En empate gana la primera regla definida.
 */
export function suggestTriage(description: string, rules: TriageRule[]): TriageSuggestion | null {
  const text = normalize(description)
  if (text.trim().length < MIN_LENGTH) return null

  let best: { rule: TriageRule; hits: number } | null = null
  for (const rule of rules) {
    if (!rule.active) continue
    const hits = rule.keywords.filter((k) => k.trim() && text.includes(normalize(k.trim()))).length
    if (hits > 0 && (!best || hits > best.hits)) best = { rule, hits }
  }
  return best ? { category: best.rule.category, priority: best.rule.priority } : null
}
