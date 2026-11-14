import type { ComponentProps } from 'react'
import { cn } from '../../lib/cn'

type Variant = 'primary' | 'secondary' | 'ghost' | 'danger' | 'danger-outline'
type Size = 'sm' | 'md' | 'lg'

const RAISED = 'shadow-[0_1px_2px_rgb(0_0_0/0.14),inset_0_1px_0_rgb(255_255_255/0.14)]'

const VARIANTS: Record<Variant, string> = {
  primary: cn(
    'border border-primary-active/40 bg-primary text-on-primary',
    RAISED,
    'enabled:hover:bg-primary-hover enabled:active:bg-primary-active enabled:active:shadow-none',
  ),
  secondary: cn(
    'border border-line bg-surface text-fg shadow-[0_1px_2px_rgb(0_0_0/0.05)]',
    'enabled:hover:border-line-strong enabled:hover:bg-surface2 enabled:active:shadow-none',
  ),
  ghost: 'border border-transparent bg-transparent text-muted enabled:hover:bg-surface2 enabled:hover:text-fg',
  danger: cn(
    'border border-err-solid-hover/40 bg-err-solid text-white',
    RAISED,
    'enabled:hover:bg-err-solid-hover enabled:active:shadow-none',
  ),
  'danger-outline': cn(
    'border border-err-fg/45 bg-surface text-err-fg',
    'enabled:hover:border-err-fg enabled:hover:bg-err-bg',
  ),
}

const SIZES: Record<Size, string> = {
  sm: 'h-7 gap-1 px-2.5 text-xs rounded-md',
  md: 'h-9 gap-1.5 px-3.5 rounded-lg max-md:h-11',
  lg: 'h-10 gap-2 px-4 rounded-lg max-md:h-11',
}

export type ButtonProps = ComponentProps<'button'> & {
  variant?: Variant
  size?: Size
  loading?: boolean
}

function Spinner() {
  return (
    <svg className="size-4 shrink-0 animate-spin" viewBox="0 0 24 24" fill="none" aria-hidden="true">
      <circle cx="12" cy="12" r="9" stroke="currentColor" strokeOpacity="0.25" strokeWidth="3" />
      <path d="M21 12a9 9 0 0 0-9-9" stroke="currentColor" strokeWidth="3" strokeLinecap="round" />
    </svg>
  )
}

export function Button({
  variant = 'secondary',
  size = 'md',
  loading = false,
  className,
  disabled,
  type = 'button',
  children,
  ...rest
}: ButtonProps) {
  // Si el llamador define su propio `display` (p. ej. `hidden md:inline-flex`) no se fuerza `inline-flex`.
  const ownDisplay = /(^|\s)(hidden|inline-flex|flex|block|grid)(\s|$)/.test(className ?? '')
  return (
    <button
      type={type}
      disabled={disabled || loading}
      aria-busy={loading || undefined}
      className={cn(
        !ownDisplay && 'inline-flex',
        'select-none items-center justify-center whitespace-nowrap font-semibold [&_svg]:shrink-0',
        'transition-[background-color,border-color,color,box-shadow,transform] duration-150 ease-out',
        'enabled:active:translate-y-px',
        'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/45 focus-visible:ring-offset-2 focus-visible:ring-offset-surface',
        'disabled:cursor-not-allowed disabled:opacity-55 disabled:shadow-none',
        VARIANTS[variant],
        SIZES[size],
        className,
      )}
      {...rest}
    >
      {loading && <Spinner />}
      {children}
    </button>
  )
}

type IconButtonProps = Omit<ButtonProps, 'children' | 'size'> & {
  label: string
  children: React.ReactNode
}

/** Botón cuadrado solo-ícono; el objetivo táctil crece a 44 px en móvil. */
export function IconButton({ label, className, variant = 'ghost', children, ...rest }: IconButtonProps) {
  return (
    <Button
      variant={variant}
      aria-label={label}
      title={label}
      className={cn('size-9 shrink-0 !p-0 max-md:size-11', className)}
      {...rest}
    >
      {children}
    </Button>
  )
}
