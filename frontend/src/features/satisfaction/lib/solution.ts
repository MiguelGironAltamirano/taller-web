import type { RequestDetailView } from '../../../shared/domain'

const PREFIX = 'Solución: '

/**
 * La solución que el técnico propuso. El backend no envía `solution`: la guarda en el historial, en la última entrada
 * hacia Resuelta ("Solución: …"). Se usa `solution` si viene (API simulada) y si no, esa entrada.
 */
export function proposedSolution(request: Pick<RequestDetailView, 'solution' | 'history'>): string | null {
  if (request.solution) return request.solution
  const resolved = [...request.history].reverse().find((h) => h.to === 'Resuelta')
  if (!resolved?.reason) return null
  return resolved.reason.startsWith(PREFIX) ? resolved.reason.slice(PREFIX.length) : resolved.reason
}
