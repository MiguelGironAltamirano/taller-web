import type { RequestDetailView, SessionUser } from '../../shared/domain'
import { ClosedCard } from './ClosedCard'
import { ResolvedCard } from './ResolvedCard'

type Props = { request: RequestDetailView; user: Pick<SessionUser, 'id' | 'role'> }

/**
 * Punto de entrada de la satisfacción en el detalle: al resolver, el solicitante dueño confirma (y califica) o
 * rechaza; al cerrar, se muestra la calificación o el cierre automático.
 */
export function SatisfactionPanel({ request, user }: Props) {
  const isOwner = user.role === 'solicitante' && request.requesterId === user.id
  if (request.state === 'Resuelta') return isOwner ? <ResolvedCard request={request} /> : null
  if (request.state === 'Cerrada') return <ClosedCard request={request} isOwner={isOwner} />
  return null
}
