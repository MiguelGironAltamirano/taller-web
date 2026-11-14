import { REQUEST_STATES } from '../../../shared/domain'
import { stateIcon, type IconName } from '../../../shared/ui'

export type NotificationVisual = { icon: IconName; tone: 'brand' | 'ok' | 'bad' | 'neutral' }

/**
 * Ícono y tono de una notificación según su texto. Los textos los arma el backend con formatos fijos
 * (`NotificationText`): asignación, reasignación, comentario, cambio de estado y cierre automático; los datos de
 * demostración agregan registro, vencimiento y resolución.
 */
export function notificationVisual(text: string): NotificationVisual {
  if (/super[óo] su SLA|vencid/i.test(text)) return { icon: 'alert', tone: 'bad' }
  if (/reasign|asign/i.test(text)) return { icon: 'userCheck', tone: 'brand' }
  if (/coment/i.test(text)) return { icon: 'message', tone: 'neutral' }
  if (/cerr[óo] automáticamente/i.test(text)) return { icon: 'lock', tone: 'neutral' }
  if (/nueva solicitud|registrad/i.test(text)) return { icon: 'inbox', tone: 'brand' }
  const state = REQUEST_STATES.find((s) => text.includes(`pasó a ${s}`))
  if (state) return { icon: stateIcon(state), tone: state === 'Resuelta' ? 'ok' : 'brand' }
  if (/resuelt/i.test(text)) return { icon: 'checkCircle', tone: 'ok' }
  return { icon: 'bell', tone: 'brand' }
}

export const VISUAL_TONE: Record<NotificationVisual['tone'], string> = {
  brand: 'bg-primary-soft text-primary-fg',
  ok: 'bg-ok-bg text-ok-fg',
  bad: 'bg-err-bg text-err-fg',
  neutral: 'bg-surface2 text-muted',
}
