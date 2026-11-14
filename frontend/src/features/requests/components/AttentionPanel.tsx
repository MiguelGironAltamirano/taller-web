import { cn } from '../../../shared/lib/cn'
import { Button, Icon, OverdueBadge, PriorityBadge, StatusBadge } from '../../../shared/ui'
import type { Attention } from '../lib/attention'
import { slaTextClass, type RowView } from '../lib/view'

export type AttentionItem = { row: RowView; attention: Attention; unseen: boolean }

type Props = {
  items: AttentionItem[]
  onOpen: (code: string) => void
  /** Muestra todas en la bandeja (filtro "Requieren mi atención"). */
  onShowAll?: () => void
  max?: number
  title?: string
}

/** Tarjetas destacadas de lo que espera una acción del usuario, con acceso directo para atenderlo. */
export function AttentionPanel({ items, onOpen, onShowAll, max = 4, title = 'Requieren tu atención' }: Props) {
  if (items.length === 0) return null
  const shown = items.slice(0, max)
  const unseen = items.filter((i) => i.unseen).length

  return (
    <section aria-labelledby="attention-title" className="overflow-hidden rounded-xl border border-line bg-surface shadow-sm">
      <div className="flex flex-wrap items-center gap-x-3 gap-y-1 border-b border-line px-4 py-3">
        <span className="grid size-9 place-items-center rounded-full bg-primary-soft text-primary-fg">
          <Icon name="bell" size={17} strokeWidth={2} />
        </span>
        <div className="min-w-0 flex-1">
          <h2 id="attention-title" className="m-0 flex items-center gap-2 text-[15px] font-semibold">
            {title}
            <span className="rounded-full bg-surface2 px-2 py-0.5 text-xs font-semibold tabular-nums text-fg ring-1 ring-inset ring-line">
              {items.length}
            </span>
          </h2>
          <p className="m-0 mt-0.5 text-xs text-muted">
            {unseen > 0 ? `${unseen} con novedades sin revisar · ` : ''}Ordenadas por urgencia
          </p>
        </div>
        {onShowAll && items.length > max && (
          <Button size="sm" variant="ghost" onClick={onShowAll} className="text-primary-fg">
            Ver las {items.length}
            <Icon name="chevR" size={14} strokeWidth={2} />
          </Button>
        )}
      </div>

      <ul className="m-0 grid list-none grid-cols-[repeat(auto-fill,minmax(280px,1fr))] gap-3 bg-surface2/40 p-3">
        {shown.map(({ row: r, attention, unseen: isNew }) => (
          <li key={r.code}>
            <article
              className={cn(
                'group flex h-full flex-col gap-2.5 rounded-lg border bg-surface p-3 transition-[border-color,box-shadow]',
                'hover:border-line-strong hover:shadow-[0_4px_14px_-6px_rgb(0_0_0/0.18)]',
                r.sla.overdue ? 'border-err-fg/35' : 'border-line',
              )}
            >
              <div className="flex items-start justify-between gap-2">
                <button
                  type="button"
                  onClick={() => onOpen(r.code)}
                  className="min-w-0 border-0 bg-transparent p-0 text-left text-fg"
                >
                  <span className="flex items-center gap-1.5 whitespace-nowrap text-[13px] font-semibold hover:underline">
                    {r.code}
                    {isNew && (
                      <span className="inline-flex items-center gap-1 rounded-full bg-primary px-1.5 py-px text-[10px] font-bold uppercase tracking-wide text-on-primary">
                        <span className="size-1.5 animate-pulse rounded-full bg-on-primary" />
                        Nuevo
                      </span>
                    )}
                  </span>
                  <span className="mt-0.5 block truncate text-xs text-muted">
                    {r.category} · {r.room}
                  </span>
                </button>
              </div>

              <div className="flex flex-wrap items-center gap-1.5">
                <PriorityBadge priority={r.priority} />
                <StatusBadge state={r.state} />
                {r.sla.overdue && <OverdueBadge />}
              </div>

              <div className="mt-auto flex items-end justify-between gap-2 border-t border-line pt-2.5">
                <div className="min-w-0">
                  <div className="truncate text-xs font-semibold text-fg">{attention.reason}</div>
                  <div className={cn('text-[11px]', slaTextClass(r.sla.tone))}>{r.sla.label}</div>
                </div>
                <Button size="sm" variant="primary" onClick={() => onOpen(r.code)} aria-label={`${attention.cta} ${r.code}`}>
                  {attention.cta}
                  <Icon name="chevR" size={14} strokeWidth={2.25} />
                </Button>
              </div>
            </article>
          </li>
        ))}
      </ul>
    </section>
  )
}
