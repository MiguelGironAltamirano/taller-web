import type { RequestState, Role } from '../../../shared/domain'

/** Acciones de flujo disponibles (spec §6). */
export type WorkflowAction = 'eval' | 'assign' | 'reassign' | 'prio' | 'noproc' | 'dup' | 'start' | 'resolve'

export type ActionConfig = {
  title: string
  message?: string
  cta: string
  needTech?: boolean
  needPrio?: boolean
  needReason?: boolean
  reasonLabel?: string
  danger?: boolean
}

export const ACTION_CONFIG: Record<WorkflowAction, ActionConfig> = {
  eval: { title: 'Pasar a evaluación', message: 'La solicitud pasará a En evaluación.', cta: 'Pasar a evaluación' },
  assign: { title: 'Asignar técnico', needTech: true, cta: 'Asignar' },
  reassign: {
    title: 'Reasignar técnico',
    needTech: true,
    needReason: true,
    reasonLabel: 'Motivo de la reasignación',
    cta: 'Reasignar',
  },
  prio: { title: 'Cambiar prioridad', needPrio: true, cta: 'Cambiar prioridad' },
  noproc: {
    title: 'Cerrar como No procede',
    needReason: true,
    reasonLabel: 'Motivo',
    cta: 'Cerrar solicitud',
    danger: true,
  },
  dup: {
    title: 'Cerrar como Duplicada',
    needReason: true,
    reasonLabel: 'Motivo (indica la solicitud original)',
    cta: 'Cerrar solicitud',
    danger: true,
  },
  start: { title: 'Iniciar atención', message: 'La solicitud pasará a En atención.', cta: 'Iniciar atención' },
  resolve: {
    title: 'Marcar como resuelta',
    needReason: true,
    reasonLabel: 'Comentario de solución',
    cta: 'Marcar como resuelta',
  },
}

const SUPERVISOR_ACTIONS: Record<RequestState, WorkflowAction[]> = {
  Registrada: ['eval', 'noproc', 'dup', 'prio'],
  'En evaluación': ['assign', 'noproc', 'dup', 'prio'],
  Asignada: ['reassign', 'prio'],
  'En atención': ['prio'],
  Resuelta: [],
  Cerrada: [],
}

/** Acciones que el rol puede ejecutar sobre una solicitud en ese estado. */
export function availableActions(role: Role, state: RequestState): WorkflowAction[] {
  if (role === 'supervisor') return SUPERVISOR_ACTIONS[state]
  if (role === 'tecnico') {
    if (state === 'Asignada') return ['start']
    if (state === 'En atención') return ['resolve']
  }
  return []
}

/**
 * Traduce un movimiento entre columnas del kanban a la acción de flujo correspondiente.
 * Devuelve `null` si la transición no está listada (el backend respondería 409).
 */
export function resolveTransition(role: Role, from: RequestState, to: RequestState): WorkflowAction | null {
  if (role === 'supervisor') {
    if (from === 'Registrada' && to === 'En evaluación') return 'eval'
    if (from === 'En evaluación' && to === 'Asignada') return 'assign'
    if ((from === 'Registrada' || from === 'En evaluación') && to === 'Cerrada') return 'noproc'
  }
  if (role === 'tecnico') {
    if (from === 'Asignada' && to === 'En atención') return 'start'
    if (from === 'En atención' && to === 'Resuelta') return 'resolve'
  }
  return null
}

/**
 * Las acciones de la interfaz que corresponden a las transiciones que el servidor permite al usuario
 * (`allowedTransitions` del contrato). Confirmar o rechazar la solución no se incluyen: las ofrece la tarjeta de
 * satisfacción del solicitante.
 */
export function actionsFromTransitions(state: RequestState, allowed: RequestState[]): WorkflowAction[] {
  const actions: WorkflowAction[] = []
  const can = (to: RequestState) => allowed.includes(to)
  switch (state) {
    case 'Registrada':
    case 'En evaluación':
      if (state === 'Registrada' && can('En evaluación')) actions.push('eval')
      if (state === 'En evaluación' && can('Asignada')) actions.push('assign')
      if (can('Cerrada')) actions.push('noproc', 'dup')
      break
    case 'Asignada':
      if (can('Asignada')) actions.push('reassign')
      if (can('En atención')) actions.push('start')
      break
    case 'En atención':
      if (can('Resuelta')) actions.push('resolve')
      break
    default:
      break
  }
  return actions
}
