import type { RowView } from './view'

const TIME_ZONE = 'America/Lima'
const MONTHS = ['Enero', 'Febrero', 'Marzo', 'Abril', 'Mayo', 'Junio', 'Julio', 'Agosto', 'Septiembre', 'Octubre', 'Noviembre', 'Diciembre']
const DAY = 86_400_000

/** Año, mes (1–12) y día en la zona horaria de Lima. */
function limaParts(ms: number): { y: number; m: number; d: number } {
  const parts = new Intl.DateTimeFormat('en-CA', { timeZone: TIME_ZONE, year: 'numeric', month: '2-digit', day: '2-digit' }).formatToParts(ms)
  const get = (t: string) => Number(parts.find((p) => p.type === t)!.value)
  return { y: get('year'), m: get('month'), d: get('day') }
}

const keyOf = (y: number, m: number, d: number) => `${y}-${String(m).padStart(2, '0')}-${String(d).padStart(2, '0')}`

/** Día calendario en Lima como `AAAA-MM-DD` (sirve para agrupar y comparar días). */
export function limaDayKey(ms: number): string {
  const p = limaParts(ms)
  return keyOf(p.y, p.m, p.d)
}

export type CalendarCell =
  | { blank: true; items: [] }
  | { blank: false; day: number; key: string; today: boolean; past: boolean; weekend: boolean; items: RowView[] }

/**
 * Cuadrícula de un mes (semana de lunes a domingo) con las solicitudes en su día de vencimiento.
 * `monthOffset` desplaza el mes respecto al actual (−1 = anterior).
 */
export function buildMonthGrid(
  rows: RowView[],
  now: number,
  monthOffset = 0,
): { title: string; cells: CalendarCell[] } {
  const today = limaParts(now)
  const todayKey = keyOf(today.y, today.m, today.d)
  const shifted = new Date(Date.UTC(today.y, today.m - 1 + monthOffset, 1))
  const y = shifted.getUTCFullYear()
  const m = shifted.getUTCMonth() + 1
  const daysInMonth = new Date(Date.UTC(y, m, 0)).getUTCDate()
  const offset = (shifted.getUTCDay() + 6) % 7

  const byDay = new Map<number, RowView[]>()
  for (const r of rows) {
    const p = limaParts(r.dueAt)
    if (p.y === y && p.m === m) byDay.set(p.d, [...(byDay.get(p.d) ?? []), r])
  }

  const cells: CalendarCell[] = Array.from({ length: offset }, () => ({ blank: true, items: [] }) as CalendarCell)
  for (let d = 1; d <= daysInMonth; d++) {
    const key = keyOf(y, m, d)
    const weekday = (offset + d - 1) % 7
    const items = (byDay.get(d) ?? []).sort((a, b) => a.dueAt - b.dueAt)
    cells.push({ blank: false, day: d, key, today: key === todayKey, past: key < todayKey, weekend: weekday >= 5, items })
  }
  while (cells.length % 7) cells.push({ blank: true, items: [] })
  return { title: `${MONTHS[m - 1]} ${y}`, cells }
}

/** Etiqueta de un día respecto a hoy: Hoy, Mañana o la fecha corta. */
export function dayLabel(key: string, now: number, fallback: string): string {
  if (key === limaDayKey(now)) return 'Hoy'
  if (key === limaDayKey(now + DAY)) return 'Mañana'
  return fallback
}
