const TIME_ZONE = 'America/Lima'

const fmt = (ms: number, options: Intl.DateTimeFormatOptions) =>
  new Date(ms).toLocaleString('es-PE', { timeZone: TIME_ZONE, ...options })

// es-PE devuelve `16-oct.`; se normaliza a `16 oct`.
const tidy = (s: string) => s.replace('-', ' ').replace('.', '')

/** `16 oct` */
export const dateShort = (ms: number) => tidy(fmt(ms, { day: '2-digit', month: 'short' }))

/** `16 oct, 10:00` */
export const dateLong = (ms: number) =>
  tidy(fmt(ms, { day: '2-digit', month: 'short', hour: '2-digit', minute: '2-digit', hour12: false }))

/** `16 de octubre de 2026` */
export const dateFull = (ms: number) => fmt(ms, { day: 'numeric', month: 'long', year: 'numeric' })

/** Número de día calendario en Lima (días desde la época), para comparar fechas sin la hora. */
function limaDayNumber(ms: number): number {
  const [year, month, day] = new Date(ms).toLocaleDateString('en-CA', { timeZone: TIME_ZONE }).split('-').map(Number)
  return Date.UTC(year, month - 1, day) / 86_400_000
}

/** Etiqueta de grupo por día calendario de Lima: Hoy, Ayer o Anteriores. */
export function dayGroup(ms: number, now: number): 'Hoy' | 'Ayer' | 'Anteriores' {
  const days = limaDayNumber(now) - limaDayNumber(ms)
  return days < 1 ? 'Hoy' : days < 2 ? 'Ayer' : 'Anteriores'
}

/** Normaliza texto para búsquedas sin tildes ni mayúsculas. */
export const normalizeText = (s: string) => s.toLowerCase().normalize('NFD').replace(/\p{Diacritic}/gu, '')
