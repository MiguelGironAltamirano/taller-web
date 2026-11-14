import { cn } from './cn'

const CONTROL = cn(
  'rounded-lg border bg-surface text-fg placeholder:text-muted transition-[border-color,box-shadow]',
  'hover:border-line-strong focus:outline-none focus:border-primary focus:ring-2 focus:ring-primary/20',
  // Inválido por `aria-invalid` (lo pone `Field`) o por la prop `invalid`: borde y foco en rojo.
  'aria-[invalid=true]:border-err-fg aria-[invalid=true]:focus:border-err-fg aria-[invalid=true]:focus:ring-err-fg/20',
  'disabled:cursor-not-allowed disabled:opacity-60',
)

/**
 * Clases comunes de inputs, selects y textareas; el borde pasa a rojo si es inválido.
 * Ocupan todo el ancho salvo que quien los usa indique su propia utilidad `w-*`
 * (Tailwind no resuelve conflictos entre `w-full` y `w-auto`).
 */
export const controlClass = (invalid?: boolean, extra?: string) =>
  cn(
    CONTROL,
    !/(^|\s)(max-)?w-/.test(extra ?? '') && 'w-full',
    invalid ? 'border-err-fg focus:border-err-fg focus:ring-err-fg/20' : 'border-line',
    extra,
  )
