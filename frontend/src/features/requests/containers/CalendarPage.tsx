import { useMemo, useState } from 'react'
import { useNavigate } from 'react-router'
import { useNow } from '../../../shared/hooks/useNow'
import { cn } from '../../../shared/lib/cn'
import { dateShort, dateWeekday, timeShort } from '../../../shared/lib/format'
import { QueryErrorState } from '../../../shared/api/QueryErrorState'
import { Button, Card, Icon, IconButton, PageHeader, PriorityBadge, type IconName } from '../../../shared/ui'
import { useSessionUser } from '../../auth'
import { useRequestRows } from '../api'
import { attentionFor, useUnseenCodes } from '../lib/attention'
import { buildMonthGrid, dayLabel, limaDayKey } from '../lib/calendar'
import { isOpen, slaTextClass, withSla, type RowView } from '../lib/view'

const HEADS = ['Lun', 'Mar', 'Mié', 'Jue', 'Vie', 'Sáb', 'Dom']
const WEEK = 7 * 86_400_000

type Tone = 'done' | 'bad' | 'warn' | 'ok'

function toneOf(r: RowView): Tone {
  if (!isOpen(r)) return 'done'
  return r.sla.overdue ? 'bad' : r.sla.tone === 'warn' ? 'warn' : 'ok'
}

const CHIP: Record<Tone, string> = {
  done: 'border-l-line-strong bg-surface2 text-muted line-through decoration-muted/50',
  bad: 'border-l-err-fg bg-err-bg text-err-fg',
  warn: 'border-l-warn bg-warn-bg text-warn-fg',
  ok: 'border-l-info-fg bg-info-bg text-info-fg',
}
const DOT: Record<Tone, string> = { done: 'bg-line-strong', bad: 'bg-err-fg', warn: 'bg-warn', ok: 'bg-info-fg' }
const BAR: Record<Tone, string> = { done: 'bg-line-strong', bad: 'bg-err-fg', warn: 'bg-warn', ok: 'bg-info-fg' }

const LEGEND: { tone: Tone; label: string }[] = [
  { tone: 'ok', label: 'En plazo' },
  { tone: 'warn', label: 'Por vencer' },
  { tone: 'bad', label: 'Vencida' },
  { tone: 'done', label: 'Finalizada' },
]

function Stat({ icon, label, value, tone }: { icon: IconName; label: string; value: number; tone: string }) {
  return (
    <div className="flex items-center gap-3 rounded-xl border border-line bg-surface px-4 py-3 shadow-sm">
      <span className={cn('grid size-10 shrink-0 place-items-center rounded-lg', tone)}>
        <Icon name={icon} size={18} strokeWidth={2} />
      </span>
      <div>
        <div className="text-2xl font-bold leading-none tabular-nums">{value}</div>
        <div className="mt-1 text-xs text-muted">{label}</div>
      </div>
    </div>
  )
}

/** Calendario mensual de vencimientos (técnico): cada solicitud aparece en el día en que vence. */
export function CalendarPage() {
  const navigate = useNavigate()
  const now = useNow()
  const user = useSessionUser()
  const unseen = useUnseenCodes()
  const rowsQuery = useRequestRows()
  const { data: rows } = rowsQuery
  const [offset, setOffset] = useState(0)
  const [selected, setSelected] = useState<string | null>(null)

  const views = useMemo(() => (rows ?? []).map((r) => withSla(r, now)), [rows, now])
  const grid = useMemo(() => buildMonthGrid(views, now, offset), [views, now, offset])
  const open = (code: string) => navigate(`/solicitudes/${code}`)

  const pending = views.filter(isOpen)
  const todayKey = limaDayKey(now)
  const stats = {
    overdue: pending.filter((r) => r.sla.overdue).length,
    today: pending.filter((r) => !r.sla.overdue && limaDayKey(r.dueAt) === todayKey).length,
    week: pending.filter((r) => !r.sla.overdue && r.dueAt <= now + WEEK).length,
  }

  const agenda = useMemo(() => {
    const source = selected
      ? views.filter((r) => limaDayKey(r.dueAt) === selected)
      : views.filter(isOpen)
    const groups = new Map<string, { label: string; items: RowView[] }>()
    for (const r of [...source].sort((a, b) => a.dueAt - b.dueAt)) {
      const key = !selected && r.sla.overdue ? 'overdue' : limaDayKey(r.dueAt)
      const label = key === 'overdue' ? 'Vencidas' : dayLabel(key, now, dateWeekday(r.dueAt))
      const g = groups.get(key) ?? { label, items: [] }
      g.items.push(r)
      groups.set(key, g)
    }
    return [...groups.entries()]
  }, [views, selected, now])

  const selectedLabel = selected
    ? dayLabel(selected, now, dateShort(new Date(`${selected}T12:00:00-05:00`).getTime()))
    : null

  return (
    <>
      <PageHeader icon="calendar" title="Calendario de vencimientos" subtitle="Tus solicitudes según su fecha límite de atención" />
      {rowsQuery.isError && !rows && (
        <Card>
          <QueryErrorState what="tus solicitudes" query={rowsQuery} compact />
        </Card>
      )}

      <div className="grid grid-cols-1 gap-3 sm:grid-cols-3">
        <Stat icon="alert" label="Vencidas" value={stats.overdue} tone="bg-err-bg text-err-fg" />
        <Stat icon="clock" label="Vencen hoy" value={stats.today} tone="bg-warn-bg text-warn-fg" />
        <Stat icon="calendar" label="Próximos 7 días" value={stats.week} tone="bg-info-bg text-info-fg" />
      </div>

      <div className="flex flex-wrap items-start gap-3.5">
        <Card className="min-w-0 flex-[2_1_560px] overflow-hidden">
          <div className="flex flex-wrap items-center gap-2 border-b border-line px-4 py-3">
            <div className="flex items-center gap-1">
              <IconButton label="Mes anterior" variant="secondary" onClick={() => setOffset((o) => o - 1)}>
                <Icon name="chevL" size={16} strokeWidth={2} />
              </IconButton>
              <IconButton label="Mes siguiente" variant="secondary" onClick={() => setOffset((o) => o + 1)}>
                <Icon name="chevR" size={16} strokeWidth={2} />
              </IconButton>
            </div>
            <h2 className="m-0 min-w-36 text-base font-semibold" aria-live="polite">
              {grid.title}
            </h2>
            {offset !== 0 && (
              <Button size="sm" variant="ghost" onClick={() => setOffset(0)}>
                Hoy
              </Button>
            )}
            <ul className="m-0 ml-auto flex list-none flex-wrap gap-x-3 gap-y-1 p-0 text-xs text-muted">
              {LEGEND.map((l) => (
                <li key={l.tone} className="flex items-center gap-1.5">
                  <span className={cn('size-2 rounded-full', DOT[l.tone])} />
                  {l.label}
                </li>
              ))}
            </ul>
          </div>

          <div className="p-2 md:p-3">
            <div className="grid grid-cols-7 gap-1 md:gap-1.5">
              {HEADS.map((h, i) => (
                <div
                  key={h}
                  className={cn(
                    'pb-1 text-center text-[11px] uppercase tracking-wider text-muted',
                    // The weekend stands out by weight, not by a lighter color (WCAG AA contrast).
                    i >= 5 ? 'font-medium' : 'font-semibold',
                  )}
                >
                  {h}
                </div>
              ))}
              {grid.cells.map((c, i) => {
                if (c.blank) return <div key={i} aria-hidden="true" className="min-h-[56px] md:min-h-28" />
                const isSel = selected === c.key
                return (
                  <div
                    key={c.key}
                    className={cn(
                      'flex min-h-[56px] min-w-0 flex-col gap-1 rounded-lg border p-1 transition-colors md:min-h-28 md:p-1.5',
                      isSel ? 'border-primary bg-primary-soft/40 ring-1 ring-primary/30' : 'border-line',
                      !isSel && (c.weekend ? 'bg-surface2/50' : 'bg-surface'),
                    )}
                  >
                    <button
                      type="button"
                      onClick={() => setSelected(isSel ? null : c.key)}
                      aria-pressed={isSel}
                      aria-label={`${c.day}${c.items.length ? `, ${c.items.length} vencimientos` : ''}`}
                      className={cn(
                        'grid size-6 shrink-0 place-items-center rounded-full border-0 text-xs font-semibold tabular-nums',
                        c.today
                          ? 'bg-primary text-on-primary hover:bg-primary-hover'
                          : cn('bg-transparent hover:bg-surface2', c.past ? 'text-muted' : 'text-fg'),
                      )}
                    >
                      {c.day}
                    </button>

                    <div className="hidden min-w-0 flex-col gap-1 md:flex">
                      {c.items.slice(0, 3).map((r) => (
                        <button
                          key={r.code}
                          type="button"
                          onClick={() => open(r.code)}
                          title={`${r.code} · ${r.category} · ${r.room}`}
                          className={cn(
                            'flex min-w-0 items-center gap-1 rounded border-0 border-l-[3px] px-1.5 py-[3px] text-left text-[11px] hover:brightness-95',
                            CHIP[toneOf(r)],
                          )}
                        >
                          <span className="shrink-0 font-semibold tabular-nums">{timeShort(r.dueAt)}</span>
                          <span className="truncate">{r.category}</span>
                        </button>
                      ))}
                      {c.items.length > 3 && (
                        <button
                          type="button"
                          onClick={() => setSelected(c.key)}
                          className="self-start rounded border-0 bg-transparent px-1 text-[11px] font-semibold text-primary-fg hover:underline"
                        >
                          +{c.items.length - 3} más
                        </button>
                      )}
                    </div>

                    {c.items.length > 0 && (
                      <div className="flex flex-wrap gap-0.5 md:hidden" aria-hidden="true">
                        {c.items.slice(0, 4).map((r) => (
                          <span key={r.code} className={cn('size-1.5 rounded-full', DOT[toneOf(r)])} />
                        ))}
                      </div>
                    )}
                  </div>
                )
              })}
            </div>
          </div>
        </Card>

        <Card className="sticky top-0 flex min-w-0 flex-[1_1_300px] flex-col">
          <div className="flex items-center justify-between gap-2 border-b border-line px-4 py-3">
            <div>
              <h2 className="m-0 text-[15px] font-semibold">
                {selectedLabel ? `Vencimientos · ${selectedLabel}` : 'Próximos vencimientos'}
              </h2>
              <p className="m-0 mt-0.5 text-xs text-muted">
                {selectedLabel ? 'Día seleccionado en el calendario' : 'Tus solicitudes abiertas, de la más urgente'}
              </p>
            </div>
            {selected && (
              <Button size="sm" variant="ghost" onClick={() => setSelected(null)}>
                Ver todas
              </Button>
            )}
          </div>

          <div className="flex max-h-[640px] flex-col gap-4 overflow-y-auto p-3">
            {agenda.length === 0 && (
              <div className="flex flex-col items-center gap-2 py-8 text-center text-muted">
                <span className="grid size-10 place-items-center rounded-full bg-ok-bg text-ok-fg">
                  <Icon name="check" size={18} strokeWidth={2.25} />
                </span>
                {selected ? 'Nada vence este día.' : 'No tienes solicitudes pendientes.'}
              </div>
            )}
            {agenda.map(([key, g]) => (
              <section key={key} aria-label={g.label}>
                <h3
                  className={cn(
                    'm-0 mb-1.5 flex items-center gap-2 text-[11px] font-semibold uppercase tracking-wider',
                    key === 'overdue' ? 'text-err-fg' : 'text-muted',
                  )}
                >
                  {g.label}
                  <span className="h-px flex-1 bg-line" />
                  <span className="tabular-nums">{g.items.length}</span>
                </h3>
                <ul className="m-0 flex list-none flex-col gap-1.5 p-0">
                  {g.items.map((r) => {
                    const tone = toneOf(r)
                    const att = attentionFor(user.role, r, unseen)
                    return (
                      <li key={r.code}>
                        <div
                          className={cn(
                            'flex items-stretch gap-3 rounded-lg border p-2.5 transition-[border-color,box-shadow] hover:border-line-strong hover:shadow-sm',
                            att ? 'border-primary/30 bg-primary-soft/30' : 'border-line bg-surface',
                          )}
                        >
                          <div className="flex w-11 shrink-0 flex-col items-center justify-center">
                            <span className="text-[13px] font-bold tabular-nums">{timeShort(r.dueAt)}</span>
                            <span className="text-[10px] uppercase text-muted">{dateShort(r.dueAt)}</span>
                          </div>
                          <span aria-hidden="true" className={cn('w-[3px] shrink-0 rounded-full', BAR[tone])} />
                          <button
                            type="button"
                            onClick={() => open(r.code)}
                            className="min-w-0 flex-1 border-0 bg-transparent p-0 text-left text-fg"
                          >
                            <span className="flex items-center gap-1.5 text-[13px] font-semibold">
                              {unseen.has(r.code) && <span className="size-2 rounded-full bg-primary" />}
                              {r.code}
                            </span>
                            <span className="block truncate text-xs text-muted">
                              {r.category} · {r.room}
                            </span>
                            <span className={cn('mt-0.5 block text-[11px] font-medium', slaTextClass(r.sla.tone))}>
                              {r.sla.label}
                            </span>
                          </button>
                          <div className="flex shrink-0 flex-col items-end justify-between gap-1.5">
                            <PriorityBadge priority={r.priority} />
                            {att && (
                              <Button size="sm" variant="primary" onClick={() => open(r.code)}>
                                {att.cta}
                              </Button>
                            )}
                          </div>
                        </div>
                      </li>
                    )
                  })}
                </ul>
              </section>
            ))}
          </div>
        </Card>
      </div>
    </>
  )
}
