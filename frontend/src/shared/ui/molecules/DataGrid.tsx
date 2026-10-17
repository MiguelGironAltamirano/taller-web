import type { ComponentProps, ReactNode } from 'react'
import { cn } from '../../lib/cn'

/**
 * Tabla basada en CSS grid del diseño: cabecera gris, filas con borde inferior y
 * desplazamiento horizontal cuando el ancho no alcanza (`minWidth`).
 */
type GridTableProps = {
  columns: string
  minWidth?: string
  heads: ReactNode[]
  children: ReactNode
  label?: string
}

export function GridTable({ columns, minWidth = '600px', heads, children, label }: GridTableProps) {
  return (
    <div className="overflow-x-auto">
      <div role="table" aria-label={label} style={{ minWidth }}>
        <div
          role="row"
          className="grid h-[38px] items-center gap-3 border-b border-line bg-surface2 px-4 text-xs font-semibold text-muted"
          style={{ gridTemplateColumns: columns }}
        >
          {heads.map((h, i) => (
            <div role="columnheader" key={i}>
              {h}
            </div>
          ))}
        </div>
        {children}
      </div>
    </div>
  )
}

type GridRowProps = Omit<ComponentProps<'div'>, 'role'> & { columns: string; interactive?: boolean }

export function GridRow({ columns, interactive, className, style, ...rest }: GridRowProps) {
  return (
    <div
      role="row"
      tabIndex={interactive ? 0 : undefined}
      className={cn(
        'grid items-center gap-3 border-b border-line px-4',
        interactive && 'cursor-pointer hover:bg-surface2',
        className,
      )}
      style={{ gridTemplateColumns: columns, ...style }}
      {...rest}
    />
  )
}
