import { cn } from './cn'

const CONTROL =
  'rounded-lg border bg-surface text-fg placeholder:text-muted disabled:cursor-not-allowed disabled:opacity-60'

/**
 * Clases comunes de inputs, selects y textareas; el borde pasa a rojo si es inválido.
 * Ocupan todo el ancho salvo que quien los usa indique su propia utilidad `w-*`
 * (Tailwind no resuelve conflictos entre `w-full` y `w-auto`).
 */
export const controlClass = (invalid?: boolean, extra?: string) =>
  cn(CONTROL, !/(^|\s)(max-)?w-/.test(extra ?? '') && 'w-full', invalid ? 'border-err-fg' : 'border-line', extra)
