import { useState } from 'react'
import type { RequestDetailView, WorkflowActionId } from '../../shared/domain'
import { cn } from '../../shared/lib/cn'
import { ActionModal } from './ActionModal'
import { ACTION_CONFIG } from './lib/transitions'

/**
 * Panel "Acciones" del detalle. Solo muestra los botones que el servidor permite
 * (`allowedActions`), nunca los deduce por su cuenta.
 */
export function ActionsPanel({ request }: { request: RequestDetailView }) {
  const [active, setActive] = useState<WorkflowActionId | null>(null)
  const actions = request.allowedActions
  if (actions.length === 0) return null

  return (
    <div className="flex flex-col gap-2 border-t border-line pt-3">
      <b className="text-[13px]">Acciones</b>
      {actions.map((a, i) => {
        const danger = ACTION_CONFIG[a].danger
        return (
          <button
            key={a}
            type="button"
            onClick={() => setActive(a)}
            className={cn(
              'h-[38px] rounded-lg px-3 text-left font-semibold max-md:h-11',
              danger && 'border border-err-fg bg-transparent text-err-fg',
              !danger && i === 0 && 'border-0 bg-primary text-on-primary',
              !danger && i !== 0 && 'border border-line bg-surface text-fg',
            )}
          >
            {ACTION_CONFIG[a].title}
          </button>
        )
      })}
      {active && <ActionModal action={active} target={request} onClose={() => setActive(null)} />}
    </div>
  )
}
