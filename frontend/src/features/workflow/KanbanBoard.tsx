import { useState } from 'react'
import { FLOW_STATES, type RequestRow, type RequestState, type Role, type WorkflowActionId } from '../../shared/domain'
import type { SlaStatus } from './lib/sla'
import { Icon, OverdueBadge, PriorityBadge, StatusBadge, useToast } from '../../shared/ui'
import { cn } from '../../shared/lib/cn'
import { ActionModal } from './ActionModal'
import { resolveTransition } from './lib/transitions'

export type KanbanItem = RequestRow & { sla: SlaStatus }

type Props<T extends KanbanItem> = {
  items: T[]
  role: Role
  onOpen: (code: string) => void
  /** Si la tarjeta espera una acción del usuario: el texto de su acción directa. */
  attentionOf?: (item: T) => { cta: string } | null
  /** Códigos con novedades sin revisar. */
  unseen?: ReadonlySet<string>
}

/**
 * Tablero por estado. Arrastrar una tarjeta solo es válido si la transición está listada para el rol
 * (spec §6); si no, se avisa y no se llama a la API. Las válidas abren el diálogo de la acción.
 */
export function KanbanBoard<T extends KanbanItem>({ items, role, onOpen, attentionOf, unseen }: Props<T>) {
  const toast = useToast()
  const [dragging, setDragging] = useState<KanbanItem | null>(null)
  const [pending, setPending] = useState<{ action: WorkflowActionId; item: KanbanItem } | null>(null)

  const drop = (to: RequestState) => {
    const item = dragging
    setDragging(null)
    if (!item || item.state === to) return
    const action = resolveTransition(role, item.state, to)
    if (!action) {
      toast(`No puedes mover una solicitud de «${item.state}» a «${to}».`, { tone: 'error', title: 'Movimiento no permitido' })
      return
    }
    setPending({ action, item })
  }

  return (
    <div className="flex items-start gap-2.5 overflow-x-auto bg-surface2 p-3">
      {FLOW_STATES.map((state) => {
        const cards = items.filter((i) => i.state === state)
        return (
          <div
            key={state}
            role="group"
            aria-label={`${state}: ${cards.length}`}
            onDragOver={(e) => e.preventDefault()}
            onDrop={(e) => {
              e.preventDefault()
              drop(state)
            }}
            className="flex min-h-40 flex-[1_0_220px] flex-col gap-2 rounded-xl border border-line bg-bg p-2"
          >
            <div className="flex items-center justify-between px-1 py-0.5">
              <StatusBadge state={state} />
              <span className="rounded-md bg-surface2 px-1.5 py-0.5 text-[11px] font-semibold tabular-nums text-muted">
                {cards.length}
              </span>
            </div>
            {cards.map((r) => {
              const att = attentionOf?.(r) ?? null
              const isNew = unseen?.has(r.code) ?? false
              return (
                <div
                  key={r.code}
                  draggable
                  tabIndex={0}
                  role="button"
                  onDragStart={(e) => {
                    setDragging(r)
                    e.dataTransfer.setData('text/plain', r.code)
                  }}
                  onClick={() => onOpen(r.code)}
                  onKeyDown={(e) => e.key === 'Enter' && onOpen(r.code)}
                  className={cn(
                    'flex cursor-grab flex-col gap-1.5 rounded-lg border p-2.5 text-[13px] shadow-sm transition-[border-color,box-shadow] hover:shadow-md active:cursor-grabbing',
                    att ? 'border-primary/40 bg-primary-soft/40' : 'border-line bg-surface hover:border-line-strong',
                  )}
                >
                  <span className="flex items-center gap-1.5 font-semibold">
                    {isNew && <span className="size-2 shrink-0 rounded-full bg-primary" title="Novedades sin revisar" />}
                    {r.code}
                  </span>
                  <span>{r.category}</span>
                  <span className="text-xs text-muted">{r.room}</span>
                  <span className="flex flex-wrap gap-1">
                    <PriorityBadge priority={r.priority} />
                    {r.sla.overdue && <OverdueBadge />}
                  </span>
                  <span className="flex items-center justify-between gap-2">
                    <span className={cn('text-[11px]', r.sla.overdue ? 'text-err-fg' : 'text-muted')}>{r.sla.label}</span>
                    {att && (
                      <span className="inline-flex items-center gap-0.5 rounded-md bg-primary px-2 py-0.5 text-[11px] font-semibold text-on-primary">
                        <span className="sr-only">Requiere tu atención: </span>
                        {att.cta}
                        <Icon name="chevR" size={12} strokeWidth={2.5} />
                      </span>
                    )}
                  </span>
                </div>
              )
            })}
          </div>
        )
      })}
      {pending && <ActionModal action={pending.action} target={pending.item} onClose={() => setPending(null)} />}
    </div>
  )
}
