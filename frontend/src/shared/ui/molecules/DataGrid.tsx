import { Children, type ComponentProps, type ReactNode } from 'react'
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
          className="grid h-10 items-center gap-3 border-b border-line bg-surface2/70 px-4 text-[11px] font-semibold uppercase tracking-wider text-muted"
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

/**
 * Fila de `GridTable`. Cada hijo directo es una celda: se envuelve en `role="cell"` con `display: contents`, así el
 * hijo sigue siendo el elemento de la cuadrícula CSS y la tabla cumple la estructura ARIA (fila → celdas).
 */
export function GridRow({ columns, interactive, className, style, children, ...rest }: GridRowProps) {
  return (
    <div
      role="row"
      tabIndex={interactive ? 0 : undefined}
      className={cn(
        'grid items-center gap-3 border-b border-line px-4',
        interactive && 'cursor-pointer transition-colors hover:bg-surface2/70',
        className,
      )}
      style={{ gridTemplateColumns: columns, ...style }}
      {...rest}
    >
      {Children.map(children, (child) =>
        child === null || child === undefined || typeof child === 'boolean' ? child : (
          <div role="cell" className="contents">
            {child}
          </div>
        ),
      )}
    </div>
  )
}
