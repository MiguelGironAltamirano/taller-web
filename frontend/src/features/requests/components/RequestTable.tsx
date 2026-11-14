import type { MouseEvent } from 'react'
import { dateShort } from '../../../shared/lib/format'
import { cn } from '../../../shared/lib/cn'
import { Avatar, Button, GridRow, GridTable, Icon, OverdueBadge, PriorityBadge, ProgressBar, StatusBadge } from '../../../shared/ui'
import type { Attention } from '../lib/attention'
import { slaBarColor, slaTextClass, type RowView } from '../lib/view'

const COLUMNS = 'minmax(190px,1.4fr) minmax(160px,1.3fr) 104px 148px minmax(150px,1.1fr) 150px 104px'

type Props = {
  rows: RowView[]
  compact: boolean
  onOpen: (code: string) => void
  attentionOf: (r: RowView) => Attention | null
  unseen: ReadonlySet<string>
}

const ATTENTION_ROW = 'bg-primary-soft/40 shadow-[inset_3px_0_0_var(--primary)] hover:bg-primary-soft/70'

function NewDot() {
  return (
    <span className="relative inline-flex size-2 shrink-0" title="Novedades sin revisar">
      <span className="absolute inset-0 animate-ping rounded-full bg-primary/50" />
      <span className="relative size-2 rounded-full bg-primary" />
      <span className="sr-only">Novedades sin revisar</span>
    </span>
  )
}

/** Tabla del listado (escritorio): una fila por solicitud, con SLA, responsable y acción directa. */
export function RequestTable({ rows, compact, onOpen, attentionOf, unseen }: Props) {
  return (
    <GridTable
      label="Solicitudes"
      columns={COLUMNS}
      minWidth="980px"
      heads={['Solicitud', 'Ubicación', 'Prioridad', 'Estado', 'Responsable', 'Plazo', '']}
    >
      {rows.map((r) => {
        const att = attentionOf(r)
        const isNew = unseen.has(r.code)
        const act = (e: MouseEvent) => {
          e.stopPropagation()
          onOpen(r.code)
        }
        return (
          <GridRow
            key={r.code}
            columns={COLUMNS}
            interactive
            onClick={() => onOpen(r.code)}
            onKeyDown={(e) => e.key === 'Enter' && onOpen(r.code)}
            className={cn('group', compact ? 'min-h-11 py-1.5 text-[13px]' : 'min-h-[60px] py-2.5', att && ATTENTION_ROW)}
          >
            <div className="min-w-0">
              <div className="flex items-center gap-2 font-semibold">
                {isNew && <NewDot />}
                {r.code}
              </div>
              <div className="truncate text-xs text-muted">{r.category}</div>
            </div>
            <div className="min-w-0" title={r.location}>
              <div className="truncate">
                {r.room} <span className="text-muted">· {r.pavilion}</span>
              </div>
              {!compact && <div className="truncate text-xs text-muted">{r.faculty}</div>}
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
                  <Avatar name={r.technician} size="xs" tone="soft" />
                  <span className="truncate">{r.technician}</span>
                </>
              ) : (
                <span className="inline-flex items-center gap-1.5 text-muted">
                  <span className="grid size-6 place-items-center rounded-full border border-dashed border-line-strong">
                    <Icon name="users" size={12} />
                  </span>
                  Sin asignar
                </span>
              )}
            </div>
            <div title={`Vence ${dateShort(r.dueAt)}`}>
              <ProgressBar value={r.sla.pct} color={slaBarColor(r.sla.tone)} label={`Plazo de ${r.code}`} />
              <div className={cn('mt-1 whitespace-nowrap text-[11px] font-medium', slaTextClass(r.sla.tone))}>
                {r.sla.label}
              </div>
            </div>
            <div className="flex justify-end">
              {att ? (
                <Button size="sm" variant="primary" onClick={act} aria-label={`${att.cta} ${r.code}`}>
                  {att.cta}
                  <Icon name="chevR" size={14} strokeWidth={2.25} />
                </Button>
              ) : (
                <Icon
                  name="chevR"
                  size={16}
                  className="text-muted opacity-0 transition-opacity group-hover:opacity-100 group-focus-visible:opacity-100"
                />
              )}
            </div>
          </GridRow>
        )
      })}
    </GridTable>
  )
}

/** Lista de tarjetas (móvil): misma información, apilada. */
export function RequestCards({ rows, onOpen, attentionOf, unseen }: Omit<Props, 'compact'>) {
  return (
    <div className="flex flex-col">
      {rows.map((r) => {
        const att = attentionOf(r)
        return (
          <div
            key={r.code}
            className={cn('flex flex-col gap-2 border-b border-line p-3.5', att && 'bg-primary-soft/40 shadow-[inset_3px_0_0_var(--primary)]')}
          >
            <button
              type="button"
              onClick={() => onOpen(r.code)}
              className="flex flex-col gap-2 border-0 bg-transparent p-0 text-left text-fg"
            >
              <div className="flex w-full items-center justify-between gap-2">
                <b className="flex items-center gap-2">
                  {unseen.has(r.code) && <NewDot />}
                  {r.code}
                </b>
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
              <div className="w-full">
                <ProgressBar value={r.sla.pct} color={slaBarColor(r.sla.tone)} label={`Plazo de ${r.code}`} />
                <span className={cn('text-[11px]', slaTextClass(r.sla.tone))}>{r.sla.label}</span>
              </div>
            </button>
            {att && (
              <div className="flex items-center justify-between gap-2">
                <span className="text-xs font-semibold">{att.reason}</span>
                <Button size="sm" variant="primary" onClick={() => onOpen(r.code)}>
                  {att.cta}
                </Button>
              </div>
            )}
          </div>
        )
      })}
    </div>
  )
}
