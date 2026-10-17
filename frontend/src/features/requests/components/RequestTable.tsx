import { dateShort } from '../../../shared/lib/format'
import { cn } from '../../../shared/lib/cn'
import { Avatar, GridRow, GridTable, Icon, OverdueBadge, PriorityBadge, ProgressBar, StatusBadge } from '../../../shared/ui'
import { slaBarColor, slaTextClass, type RowView } from '../lib/view'

const COLUMNS = '130px 1.2fr 1.5fr 90px 120px 1.2fr 150px 100px 24px'

type Props = { rows: RowView[]; compact: boolean; onOpen: (code: string) => void }

/** Tabla del listado (escritorio): una fila por solicitud, con SLA y responsable. */
export function RequestTable({ rows, compact, onOpen }: Props) {
  return (
    <GridTable
      label="Solicitudes"
      columns={COLUMNS}
      minWidth="980px"
      heads={['Código', 'Categoría', 'Ubicación', 'Prioridad', 'Estado', 'Responsable', 'SLA', 'Fecha', '']}
    >
      {rows.map((r) => (
        <GridRow
          key={r.code}
          columns={COLUMNS}
          interactive
          onClick={() => onOpen(r.code)}
          onKeyDown={(e) => e.key === 'Enter' && onOpen(r.code)}
          className={compact ? 'h-10 text-[13px]' : 'h-12'}
        >
          <div className="font-semibold">{r.code}</div>
          <div className="truncate">{r.category}</div>
          <div className="truncate text-muted" title={r.location}>
            {r.room} · {r.pavilion}
          </div>
          <div>
            <PriorityBadge priority={r.priority} />
          </div>
          <div>
            <StatusBadge state={r.state} />
          </div>
          <div className="flex min-w-0 items-center gap-2">
            {r.technician ? (
              <>
                <Avatar name={r.technician} size="xs" />
                <span className="truncate">{r.technician}</span>
              </>
            ) : (
              <span className="text-muted">Sin asignar</span>
            )}
          </div>
          <div>
            <ProgressBar value={r.sla.pct} color={slaBarColor(r.sla.tone)} label={`SLA de ${r.code}`} />
            <div className={cn('mt-0.5 whitespace-nowrap text-[11px]', slaTextClass(r.sla.tone))}>{r.sla.label}</div>
          </div>
          <div className="text-muted">{dateShort(r.dueAt)}</div>
          <div className="text-muted">
            <Icon name="chevR" size={16} />
          </div>
        </GridRow>
      ))}
    </GridTable>
  )
}

/** Lista de tarjetas (móvil): misma información, apilada. */
export function RequestCards({ rows, onOpen }: Pick<Props, 'rows' | 'onOpen'>) {
  return (
    <div className="flex flex-col">
      {rows.map((r) => (
        <button
          key={r.code}
          type="button"
          onClick={() => onOpen(r.code)}
          className="flex flex-col gap-2 border-0 border-b border-line bg-transparent p-3.5 text-left text-fg"
        >
          <div className="flex justify-between gap-2">
            <b>{r.code}</b>
            <span className="text-xs text-muted">{dateShort(r.dueAt)}</span>
          </div>
          <div>
            {r.category} <span className="text-muted">· {r.room}</span>
          </div>
          <div className="flex flex-wrap gap-1.5">
            <StatusBadge state={r.state} />
            <PriorityBadge priority={r.priority} />
            {r.sla.overdue && <OverdueBadge />}
          </div>
          <div>
            <ProgressBar value={r.sla.pct} color={slaBarColor(r.sla.tone)} label={`SLA de ${r.code}`} />
            <span className={cn('text-[11px]', slaTextClass(r.sla.tone))}>{r.sla.label}</span>
          </div>
        </button>
      ))}
    </div>
  )
}
