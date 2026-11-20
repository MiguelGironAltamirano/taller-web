import { isFinalState, type RequestRow } from '../../../shared/domain'
import { formatDuration, slaStatus, type SlaStatus, type SlaTone } from '../../workflow/lib/sla'

export type RowView = RequestRow & { sla: SlaStatus; location: string }

/**
 * Plazo que ve el solicitante: la fecha estimada con la prioridad que eligió. Nunca se marca como vencida;
 * pasada la fecha solo indica que la solicitud sigue en atención.
 */
function estimatedStatus(row: RequestRow, now: number): SlaStatus {
  if (isFinalState(row.state)) return slaStatus(row, now)
  const pct = Math.min(100, Math.max(0, ((now - row.createdAt) / (row.dueAt - row.createdAt)) * 100))
  return row.dueAt < now
    ? { overdue: false, pct, label: 'Sigue en atención', tone: 'ok' }
    : { overdue: false, pct, label: `Estimada en ${formatDuration(row.dueAt - now)}`, tone: 'ok' }
}

/** Agrega el estado de SLA y la ubicación legible a una fila de bandeja. */
export function withSla(row: RequestRow, now: number): RowView {
  return {
    ...row,
    sla: row.estimatedDue
      ? estimatedStatus(row, now)
      : slaStatus({ state: row.state, createdAt: row.createdAt, dueAt: row.dueAt }, now),
    location: `${row.faculty} → ${row.pavilion} → ${row.room}`,
  }
}

const BAR: Record<SlaTone, string> = {
  ok: 'var(--good)',
  warn: 'var(--warn)',
  bad: 'var(--bad)',
  done: 'var(--good)',
}

export const slaBarColor = (tone: SlaTone) => BAR[tone]
export const slaTextClass = (tone: SlaTone) =>
  tone === 'bad' ? 'text-err-fg' : tone === 'warn' ? 'text-warn-fg' : 'text-muted'

/** Solicitudes abiertas = aún no llegaron a "Resuelta". */
export const isOpen = (r: RequestRow) => r.state !== 'Resuelta' && r.state !== 'Cerrada'
