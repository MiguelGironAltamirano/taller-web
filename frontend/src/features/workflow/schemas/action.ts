import { z } from 'zod'
import type { ChangePriorityBody, TransitionBody } from '../../../shared/api/types'
import type { WorkflowActionId } from '../../../shared/domain'
import type { ActionConfig } from '../lib/transitions'

export type ActionValues = { technicianId: string; priorityId: string; reason: string }

export const REASON_MIN = 5

/** Esquema del formulario de una acción: solo exige los campos que esa acción pide. */
export function actionSchema(config: ActionConfig) {
  return z.object({
    technicianId: config.needTech ? z.string().min(1, 'Selecciona un técnico.') : z.string(),
    priorityId: config.needPrio ? z.string().min(1, 'Selecciona una prioridad.') : z.string(),
    reason: config.needReason
      ? z.string().trim().min(REASON_MIN, `Indica un motivo de al menos ${REASON_MIN} caracteres.`)
      : z.string(),
  })
}

export type ApiCall =
  | { kind: 'transition'; body: TransitionBody }
  | { kind: 'priority'; body: ChangePriorityBody }

/** Traduce la acción de la interfaz al contrato de la API (`/transitions` o `/priority`). */
export function toApiCall(action: WorkflowActionId, v: ActionValues): ApiCall {
  const reason = v.reason.trim()
  switch (action) {
    case 'eval':
      return { kind: 'transition', body: { to: 'EN_EVALUACION' } }
    case 'assign':
      return { kind: 'transition', body: { to: 'ASIGNADA', technicianId: Number(v.technicianId) } }
    case 'reassign':
      return { kind: 'transition', body: { to: 'ASIGNADA', technicianId: Number(v.technicianId), reason } }
    case 'start':
      return { kind: 'transition', body: { to: 'EN_ATENCION' } }
    case 'resolve':
      return { kind: 'transition', body: { to: 'RESUELTA', reason } }
    case 'noproc':
      return { kind: 'transition', body: { to: 'CERRADA', closureReason: 'NO_PROCEDE', reason } }
    case 'dup':
      return { kind: 'transition', body: { to: 'CERRADA', closureReason: 'DUPLICADA', reason } }
    case 'prio':
      return { kind: 'priority', body: { priorityId: Number(v.priorityId) } }
  }
}
