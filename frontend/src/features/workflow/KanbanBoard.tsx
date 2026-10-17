import { useState } from 'react'
import { REQUEST_STATES, type RequestRow, type RequestState, type Role, type WorkflowActionId } from '../../shared/domain'
import type { SlaStatus } from './lib/sla'
import { OverdueBadge, PriorityBadge, useToast } from '../../shared/ui'
import { cn } from '../../shared/lib/cn'
import { ActionModal } from './ActionModal'
import { resolveTransition } from './lib/transitions'

export type KanbanItem = RequestRow & { sla: SlaStatus }

type Props = {
  items: KanbanItem[]
  role: Role
  onOpen: (code: string) => void
}

/**
 * Tablero por estado. Arrastrar una tarjeta solo es válido si la transición está listada para el rol
 * (spec §6); si no, se avisa y no se llama a la API. Las válidas abren el diálogo de la acción.
 */
export function KanbanBoard({ items, role, onOpen }: Props) {
  const toast = useToast()
  const [dragging, setDragging] = useState<KanbanItem | null>(null)
  const [pending, setPending] = useState<{ action: WorkflowActionId; item: KanbanItem } | null>(null)

  const drop = (to: RequestState) => {
    const item = dragging
    setDragging(null)
    if (!item || item.state === to) return
    const action = resolveTransition(role, item.state, to)
    if (!action) {
      toast(`Transición no válida: ${item.state} → ${to}`, { bad: true })
      return
    }
    setPending({ action, item })
  }

  return (
    <div className="flex items-start gap-2.5 overflow-x-auto bg-surface2 p-3">
      {REQUEST_STATES.map((state) => {
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
            className="flex min-h-40 flex-[1_0_210px] flex-col gap-2 rounded-[10px] border border-line bg-bg p-2"
          >
            <div className="flex justify-between px-1 py-0.5 text-xs font-semibold text-muted">
              <span>{state}</span>
              <span>{cards.length}</span>
            </div>
            {cards.map((r) => (
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
                className="flex cursor-grab flex-col gap-1.5 rounded-lg border border-line bg-surface p-2.5 text-[13px]"
              >
                <b>{r.code}</b>
                <span>{r.category}</span>
                <span className="text-xs text-muted">{r.room}</span>
                <span className="flex flex-wrap gap-1">
                  <PriorityBadge priority={r.priority} />
                  {r.sla.overdue && <OverdueBadge />}
                </span>
                <span className={cn('text-[11px]', r.sla.overdue ? 'text-err-fg' : 'text-muted')}>{r.sla.label}</span>
              </div>
            ))}
          </div>
        )
      })}
      {pending && <ActionModal action={pending.action} target={pending.item} onClose={() => setPending(null)} />}
    </div>
  )
}
