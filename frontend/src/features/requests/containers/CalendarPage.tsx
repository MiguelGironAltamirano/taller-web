import { useMemo } from 'react'
import { useNavigate } from 'react-router'
import { useNow } from '../../../shared/hooks/useNow'
import { cn } from '../../../shared/lib/cn'
import { Card, CardTitle, PageHeader, PriorityBadge } from '../../../shared/ui'
import { useRequestRows } from '../api'
import { buildMonthGrid } from '../lib/calendar'
import { isOpen, slaTextClass, withSla, type RowView } from '../lib/view'

const HEADS = ['Lun', 'Mar', 'Mié', 'Jue', 'Vie', 'Sáb', 'Dom']

function chipClass(r: RowView): string {
  if (r.state === 'Resuelta' || r.state === 'Cerrada') return 'bg-surface2 text-muted'
  return r.sla.overdue ? 'bg-err-bg text-err-fg' : 'bg-info-bg text-info-fg'
}

/** Calendario mensual de vencimientos (técnico): cada solicitud aparece en el día en que vence. */
export function CalendarPage() {
  const navigate = useNavigate()
  const now = useNow()
  const { data: rows } = useRequestRows()
  const views = useMemo(() => (rows ?? []).map((r) => withSla(r, now)), [rows, now])
  const grid = useMemo(() => buildMonthGrid(views, now), [views, now])
  const upcoming = views.filter(isOpen).sort((a, b) => a.dueAt - b.dueAt).slice(0, 6)
  const open = (code: string) => navigate(`/solicitudes/${code}`)

  return (
    <>
      <PageHeader title="Calendario de vencimientos" subtitle={grid.title} />
      <Card className="p-2">
        <div className="grid grid-cols-7 gap-1">
          {HEADS.map((h) => (
            <div key={h} className="p-1 text-center text-xs font-semibold text-muted">
              {h}
            </div>
          ))}
          {grid.cells.map((c, i) => (
            <div
              key={i}
              className={cn(
                'flex min-h-[52px] flex-col gap-[3px] rounded-lg border border-line p-1 md:min-h-24',
                c.blank ? 'bg-transparent opacity-30' : c.today ? 'bg-primary-soft' : 'bg-surface',
              )}
            >
              {!c.blank && <div className={cn('text-xs', c.today ? 'font-bold' : 'font-medium')}>{c.day}</div>}
              {/* Escritorio: chips con el código; móvil: solo el contador */}
              <div className="hidden flex-col gap-[3px] md:flex">
                {c.items.slice(0, 2).map((r) => (
                  <button
                    key={r.code}
                    type="button"
                    onClick={() => open(r.code)}
                    className={cn('block w-full cursor-pointer truncate rounded border-0 px-[5px] py-0.5 text-left text-[11px]', chipClass(r))}
                  >
                    {r.code.slice(-6)} · {r.category}
                  </button>
                ))}
                {c.items.length > 2 && <span className="text-[11px] text-muted">+{c.items.length - 2} más</span>}
              </div>
              {c.items.length > 0 && (
                <span className="grid h-[18px] min-w-[18px] self-start place-items-center rounded-full bg-primary px-[5px] text-[11px] font-bold text-on-primary md:hidden">
                  {c.items.length}
                </span>
              )}
            </div>
          ))}
        </div>
      </Card>
      <Card className="p-4">
        <CardTitle className="mb-2">Próximos vencimientos</CardTitle>
        {upcoming.length === 0 && <div className="text-muted">No tienes solicitudes pendientes.</div>}
        {upcoming.map((r) => (
          <button
            key={r.code}
            type="button"
            onClick={() => open(r.code)}
            className="flex w-full flex-wrap items-center gap-2.5 border-0 border-t border-line bg-transparent px-1 py-2.5 text-left text-fg"
          >
            <b className="w-[130px] text-[13px]">{r.code}</b>
            <span className="min-w-[140px] flex-1">
              {r.category} <span className="text-muted">· {r.room}</span>
            </span>
            <PriorityBadge priority={r.priority} />
            <span className={cn('text-xs font-semibold', slaTextClass(r.sla.tone))}>{r.sla.label}</span>
          </button>
        ))}
      </Card>
    </>
  )
}
