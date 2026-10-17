import type { ComponentProps } from 'react'
import { cn } from '../../lib/cn'

type Variant = 'primary' | 'secondary' | 'ghost' | 'danger' | 'danger-outline'
type Size = 'sm' | 'md' | 'lg'

const VARIANTS: Record<Variant, string> = {
  primary: 'border-0 bg-primary text-on-primary hover:brightness-110',
  secondary: 'border border-line bg-surface text-fg hover:bg-surface2',
  ghost: 'border-0 bg-transparent text-muted hover:bg-surface2',
  danger: 'border-0 bg-err-solid text-white hover:brightness-110',
  'danger-outline': 'border border-err-fg bg-transparent text-err-fg hover:bg-surface2',
}

const SIZES: Record<Size, string> = {
  sm: 'h-7 px-2.5 text-xs rounded-md',
  md: 'h-9 px-3.5 rounded-lg max-md:h-11',
  lg: 'h-10 px-4 rounded-lg max-md:h-11',
}

export type ButtonProps = ComponentProps<'button'> & {
  variant?: Variant
  size?: Size
  loading?: boolean
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
  return (
    <button
      type={type}
      disabled={disabled || loading}
      aria-busy={loading || undefined}
      className={cn(
        'inline-flex items-center justify-center gap-1.5 font-semibold transition-[filter,background-color] disabled:cursor-not-allowed disabled:opacity-60',
        VARIANTS[variant],
        SIZES[size],
        className,
      )}
      {...rest}
    >
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
