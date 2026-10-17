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

/** Etiqueta de grupo por antigüedad: Hoy, Ayer o Anteriores. */
export function dayGroup(ms: number, now: number): 'Hoy' | 'Ayer' | 'Anteriores' {
  const days = Math.floor((now - ms) / 86_400_000)
  return days < 1 ? 'Hoy' : days < 2 ? 'Ayer' : 'Anteriores'
}

/** Normaliza texto para búsquedas sin tildes ni mayúsculas. */
export const normalizeText = (s: string) => s.toLowerCase().normalize('NFD').replace(/\p{Diacritic}/gu, '')
