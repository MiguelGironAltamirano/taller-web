import type { RequestRow } from '../../../shared/domain'
import { slaStatus, type SlaStatus, type SlaTone } from '../../workflow/lib/sla'

export type RowView = RequestRow & { sla: SlaStatus; location: string }

/** Agrega el estado de SLA y la ubicación legible a una fila de bandeja. */
export function withSla(row: RequestRow, now: number): RowView {
  return {
    ...row,
    sla: slaStatus({ state: row.state, createdAt: row.createdAt, dueAt: row.dueAt }, now),
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
export const slaTextClass = (tone: SlaTone) => (tone === 'bad' ? 'text-err-fg' : 'text-muted')

/** Solicitudes abiertas = aún no llegaron a "Resuelta". */
export const isOpen = (r: RequestRow) => r.state !== 'Resuelta' && r.state !== 'Cerrada'
