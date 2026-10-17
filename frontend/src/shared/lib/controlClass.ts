import { cn } from './cn'

const CONTROL =
  'w-full rounded-lg border bg-surface text-fg placeholder:text-muted disabled:cursor-not-allowed disabled:opacity-60'

/** Clases comunes de inputs, selects y textareas; el borde pasa a rojo si es inválido. */
export const controlClass = (invalid?: boolean, extra?: string) =>
  cn(CONTROL, invalid ? 'border-err-fg' : 'border-line', extra)
