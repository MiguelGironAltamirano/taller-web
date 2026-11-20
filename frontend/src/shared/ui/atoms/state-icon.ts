import type { RequestState } from '../../domain'
import type { IconName } from './Icon'

const STATE_ICON: Record<RequestState, IconName> = {
  Registrada: 'inbox',
  'En evaluación': 'clipboard',
  Asignada: 'userCheck',
  'En atención': 'wrench',
  Resuelta: 'checkCircle',
  Cerrada: 'lock',
  Cancelada: 'ban',
}

export const stateIcon = (state: RequestState): IconName => STATE_ICON[state]
