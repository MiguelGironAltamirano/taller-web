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

/** Minúsculas, sin tildes y con espacios simples, igual que el backend. */
export const normalizeTriageText = (s: string) =>
  s.toLowerCase().normalize('NFD').replace(/\p{M}/gu, '').replace(/\s+/g, ' ').trim()

const escapeRegExp = (s: string) => s.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')

/** La palabra clave aparece como palabra o frase completa (letras y dígitos no la continúan). */
const appears = (text: string, keyword: string) =>
  new RegExp(`(?<![\\p{L}\\p{N}])${escapeRegExp(keyword)}(?![\\p{L}\\p{N}])`, 'u').test(text)

/**
 * Algoritmo del contrato (spec §5.1): entre las reglas activas gana la de más palabras clave coincidentes
 * (palabra o frase completa, sin distinguir mayúsculas ni tildes); en empate, la de menor `id`.
 * Sin coincidencias devuelve `null`. Quien llama filtra las reglas cuya categoría ya no está activa.
 */
export function matchTriage<R extends Pick<TriageRule, 'id' | 'keywords' | 'active'>>(description: string, rules: R[]): R | null {
  const text = normalizeTriageText(description)
  let best: { rule: R; hits: number } | null = null
  for (const rule of rules) {
    if (!rule.active) continue
    const keywords = new Set(rule.keywords.map(normalizeTriageText).filter(Boolean))
    const hits = [...keywords].filter((k) => appears(text, k)).length
    if (hits > 0 && (!best || hits > best.hits || (hits === best.hits && rule.id < best.rule.id))) best = { rule, hits }
  }
  return best?.rule ?? null
}

/** Sugerencia local (probador del administrador): no sugiere con descripciones muy cortas. */
export function suggestTriage(description: string, rules: TriageRule[]): TriageSuggestion | null {
  if (description.trim().length < MIN_LENGTH) return null
  const rule = matchTriage(description, rules)
  return rule ? { category: rule.category, priority: rule.priority } : null
}
