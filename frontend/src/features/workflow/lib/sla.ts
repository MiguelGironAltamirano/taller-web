import { isFinalState, type RequestState } from '../../../shared/domain'

const HOUR = 3_600_000

export type SlaTone = 'ok' | 'warn' | 'bad' | 'done'

export type SlaStatus = {
  overdue: boolean
  /** Porcentaje del plazo consumido (0–100). */
  pct: number
  label: string
  tone: SlaTone
}

/** Fecha de vencimiento = fecha de registro + horas de SLA de la prioridad (spec §5.1). */
export function computeDueAt(createdAt: number, slaHours: number): number {
  return createdAt + slaHours * HOUR
}

/** Horas por debajo de 48 h; días a partir de ahí. */
export function formatDuration(ms: number): string {
  const hours = Math.round(Math.abs(ms) / HOUR)
  return hours >= 48 ? `${Math.round(hours / 24)} d` : `${hours} h`
}

type SlaInput = { state: RequestState; createdAt: number; dueAt: number }

/** Una solicitud está vencida si no llegó a "Resuelta" antes de su fecha de vencimiento. */
export function slaStatus({ state, createdAt, dueAt }: SlaInput, now: number): SlaStatus {
  if (isFinalState(state)) {
    return { overdue: false, pct: 100, label: state === 'Resuelta' ? 'Atendida' : 'Cerrada', tone: 'done' }
  }
  const pct = Math.min(100, Math.max(0, ((now - createdAt) / (dueAt - createdAt)) * 100))
  if (dueAt < now) {
    return { overdue: true, pct, label: `Vencida hace ${formatDuration(now - dueAt)}`, tone: 'bad' }
  }
  return {
    overdue: false,
    pct,
    label: `Vence en ${formatDuration(dueAt - now)}`,
    tone: pct >= 80 ? 'warn' : 'ok',
  }
}
