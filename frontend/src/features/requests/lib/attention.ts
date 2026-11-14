import { useMemo } from 'react'
import { isFinalState, type Priority, type RequestRow, type Role } from '../../../shared/domain'
import { useNotifications } from '../../notifications'
import type { RowView } from './view'

export type Attention = {
  /** Qué espera la solicitud, en pocas palabras. */
  reason: string
  /** Texto del botón de acción directa. */
  cta: string
}

/** Lo que cada rol debe hacer según el estado (spec §6); `null` si la solicitud no espera nada de él. */
function pendingAction(role: Role, r: RequestRow): Attention | null {
  if (role === 'supervisor') {
    if (r.state === 'Registrada') return { reason: 'Nueva, por evaluar', cta: 'Evaluar' }
    if (r.state === 'En evaluación') return { reason: 'Falta asignar técnico', cta: 'Asignar' }
  }
  if (role === 'tecnico' && r.state === 'Asignada') return { reason: 'Asignada a ti, por iniciar', cta: 'Atender' }
  if (role === 'solicitante' && r.state === 'Resuelta') return { reason: 'Confirma la solución', cta: 'Revisar' }
  return null
}

/** Atención requerida: una acción pendiente del rol o un cambio que aún no vio (notificación sin leer). */
export function attentionFor(role: Role, r: RequestRow, unseen: ReadonlySet<string>): Attention | null {
  const pending = pendingAction(role, r)
  if (pending) return pending
  if (unseen.has(r.code) && !isFinalState(r.state)) return { reason: 'Tiene novedades', cta: 'Ver' }
  return null
}

/** Códigos de solicitud con notificaciones sin leer (se refrescan cada 30 s). */
export function useUnseenCodes(): ReadonlySet<string> {
  const { data } = useNotifications()
  return useMemo(
    () => new Set((data ?? []).filter((n) => !n.read && n.requestCode).map((n) => n.requestCode as string)),
    [data],
  )
}

const PRIORITY_RANK: Record<Priority, number> = { Crítica: 0, Alta: 1, Media: 2, Baja: 3 }

/** Lo nuevo primero, luego lo vencido, la prioridad y lo que vence antes. */
export function sortByUrgency(rows: RowView[], unseen: ReadonlySet<string>): RowView[] {
  return [...rows].sort(
    (a, b) =>
      Number(unseen.has(b.code)) - Number(unseen.has(a.code)) ||
      Number(b.sla.overdue) - Number(a.sla.overdue) ||
      PRIORITY_RANK[a.priority] - PRIORITY_RANK[b.priority] ||
      a.dueAt - b.dueAt,
  )
}
