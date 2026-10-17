import type { RowView } from './view'

const TIME_ZONE = 'America/Lima'
const MONTHS = ['Enero', 'Febrero', 'Marzo', 'Abril', 'Mayo', 'Junio', 'Julio', 'Agosto', 'Septiembre', 'Octubre', 'Noviembre', 'Diciembre']

/** Año, mes (1–12) y día en la zona horaria de Lima. */
function limaParts(ms: number): { y: number; m: number; d: number } {
  const parts = new Intl.DateTimeFormat('en-CA', { timeZone: TIME_ZONE, year: 'numeric', month: '2-digit', day: '2-digit' }).formatToParts(ms)
  const get = (t: string) => Number(parts.find((p) => p.type === t)!.value)
  return { y: get('year'), m: get('month'), d: get('day') }
}

export type CalendarCell =
  | { blank: true; items: [] }
  | { blank: false; day: number; today: boolean; items: RowView[] }

/** Cuadrícula del mes actual (semana de lunes a domingo) con las solicitudes en su día de vencimiento. */
export function buildMonthGrid(rows: RowView[], now: number): { title: string; cells: CalendarCell[] } {
  const today = limaParts(now)
  const daysInMonth = new Date(Date.UTC(today.y, today.m, 0)).getUTCDate()
  const firstWeekday = new Date(Date.UTC(today.y, today.m - 1, 1)).getUTCDay() // 0 = domingo
  const offset = (firstWeekday + 6) % 7

  const byDay = new Map<number, RowView[]>()
  for (const r of rows) {
    const p = limaParts(r.dueAt)
    if (p.y === today.y && p.m === today.m) byDay.set(p.d, [...(byDay.get(p.d) ?? []), r])
  }

  const cells: CalendarCell[] = Array.from({ length: offset }, () => ({ blank: true, items: [] }) as CalendarCell)
  for (let d = 1; d <= daysInMonth; d++) {
    cells.push({ blank: false, day: d, today: d === today.d, items: byDay.get(d) ?? [] })
  }
  while (cells.length % 7) cells.push({ blank: true, items: [] })
  return { title: `${MONTHS[today.m - 1]} ${today.y}`, cells }
}
