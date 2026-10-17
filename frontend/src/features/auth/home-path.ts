import type { Role } from '../../shared/api/types'

export function homePathFor(role: Role): string {
  return role === 'SOLICITANTE' ? '/solicitudes' : '/bandeja'
}
