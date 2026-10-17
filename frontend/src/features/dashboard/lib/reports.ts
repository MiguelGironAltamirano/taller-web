import type { TeamMemberLoad } from '../../../shared/api/types'
import { PRIORITIES, REQUEST_STATES } from '../../../shared/domain'
import { buildCsv } from '../../../shared/lib/csv'
import { dateLong } from '../../../shared/lib/format'
import type { RowView } from '../../requests'

export type ReportId = 'estado' | 'sla' | 'carga' | 'vencidas'

export const REPORTS: { id: ReportId; title: string; description: string }[] = [
  { id: 'estado', title: 'Solicitudes por estado', description: 'Totales y detalle por estado en el periodo' },
  { id: 'sla', title: 'Cumplimiento de SLA', description: 'Solicitudes dentro y fuera de plazo por prioridad' },
  { id: 'carga', title: 'Carga por técnico', description: 'Asignaciones y tiempos promedio por técnico' },
  { id: 'vencidas', title: 'Solicitudes vencidas', description: 'Listado con tiempo de retraso y responsable' },
]

const today = () => new Date().toISOString().slice(0, 10)

/** Genera el CSV de un reporte a partir de las solicitudes del área y la carga del equipo. */
export function buildReport(id: ReportId, rows: RowView[], team: TeamMemberLoad[]): { filename: string; csv: string } {
  switch (id) {
    case 'estado':
      return {
        filename: `solicitudes-por-estado-${today()}.csv`,
        csv: buildCsv(['Estado', 'Solicitudes'], REQUEST_STATES.map((s) => [s, rows.filter((r) => r.state === s).length])),
      }
    case 'sla':
      return {
        filename: `cumplimiento-sla-${today()}.csv`,
        csv: buildCsv(
          ['Prioridad', 'Total', 'Dentro de plazo', 'Fuera de plazo', 'Cumplimiento (%)'],
          PRIORITIES.map((p) => {
            const list = rows.filter((r) => r.priority === p)
            const late = list.filter((r) => r.sla.overdue).length
            const pct = list.length ? Math.round(((list.length - late) / list.length) * 100) : 100
            return [p, list.length, list.length - late, late, pct]
          }),
        ),
      }
    case 'carga':
      return {
        filename: `carga-por-tecnico-${today()}.csv`,
        csv: buildCsv(
          ['Técnico', 'Asignadas', 'En atención', 'Vencidas', 'Capacidad'],
          team.map((t) => [t.fullName, t.assigned, t.inProgress, t.overdue, t.capacity]),
        ),
      }
    case 'vencidas':
      return {
        filename: `solicitudes-vencidas-${today()}.csv`,
        csv: buildCsv(
          ['Código', 'Categoría', 'Responsable', 'Prioridad', 'Vencimiento', 'Retraso'],
          rows
            .filter((r) => r.sla.overdue)
            .map((r) => [r.code, r.category, r.technician ?? 'Sin asignar', r.priority, dateLong(r.dueAt), r.sla.label]),
        ),
      }
  }
}

/** Color de la variación de un indicador: sube bien si "más es mejor", y al revés. */
export function kpiTone(deltaPct: number, higherIsBetter: boolean): 'good' | 'bad' {
  return deltaPct > 0 === higherIsBetter ? 'good' : 'bad'
}
