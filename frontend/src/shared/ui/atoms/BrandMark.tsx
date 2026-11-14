import { cn } from '../../lib/cn'

type BrandMarkProps = {
  size?: number
  /** `solid`: fondo guinda (sobre superficies claras u oscuras); `inverse`: fondo blanco (sobre el panel guinda). */
  tone?: 'solid' | 'inverse'
  className?: string
}

/**
 * Monograma de la plataforma. No reemplaza al escudo oficial de la UNMSM: si el equipo obtiene el archivo oficial,
 * basta con dibujarlo aquí y todas las vistas (barra lateral, menú móvil, login y favicon) lo usarán.
 */
export function BrandMark({ size = 32, tone = 'solid', className }: BrandMarkProps) {
  const fg = tone === 'solid' ? '#ffffff' : 'var(--primary)'
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 32 32"
      aria-hidden="true"
      focusable="false"
      className={cn('shrink-0', className)}
    >
      <rect width="32" height="32" rx="8" fill={tone === 'solid' ? 'var(--primary)' : '#ffffff'} />
      <rect x="0.5" y="0.5" width="31" height="31" rx="7.5" fill="none" stroke="rgb(0 0 0 / 0.08)" />
      <path d="M10.5 9v7.5a5.5 5.5 0 0 0 11 0V9" fill="none" stroke={fg} strokeWidth="3" strokeLinecap="round" />
    </svg>
  )
}

/** Logotipo completo: monograma con el nombre de la plataforma. */
export function BrandLogo({ compact = false, inverse = false }: { compact?: boolean; inverse?: boolean }) {
  return (
    <span className="flex min-w-0 items-center gap-2.5">
      <BrandMark size={compact ? 32 : 34} tone={inverse ? 'inverse' : 'solid'} />
      {!compact && (
        <span className="min-w-0 leading-tight">
          <span className={cn('block truncate text-[13.5px] font-semibold', inverse ? 'text-white' : 'text-fg')}>
            Servicios Universitarios
          </span>
          <span
            className={cn(
              'block text-[10.5px] font-semibold uppercase tracking-[0.14em]',
              inverse ? 'text-white/70' : 'text-muted',
            )}
          >
            UNMSM
          </span>
        </span>
      )}
    </span>
  )
}
