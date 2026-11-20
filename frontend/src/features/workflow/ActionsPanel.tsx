import { useState } from 'react'
import { stateToStatus } from '../../shared/api/mappers'
import { lastCancellation, type RequestDetailView, type WorkflowActionId } from '../../shared/domain'
import { Button, Icon, type IconName } from '../../shared/ui'
import { ActionModal } from './ActionModal'
import { ACTION_CONFIG } from './lib/transitions'

const ACTION_ICON: Record<WorkflowActionId, IconName> = {
  eval: 'clipboard',
  assign: 'userCheck',
  reassign: 'users',
  prio: 'flag',
  noproc: 'ban',
  dup: 'copy',
  start: 'play',
  resolve: 'checkCircle',
  cancel: 'ban',
  restore: 'inbox',
}

/**
 * Panel "Acciones" del detalle. Solo muestra los botones que el servidor permite
 * (`allowedActions`), nunca los deduce por su cuenta.
 */
export function ActionsPanel({ request }: { request: RequestDetailView }) {
  const [active, setActive] = useState<WorkflowActionId | null>(null)
  const actions = request.allowedActions
  if (actions.length === 0) return null

  const main = actions.filter((a) => !ACTION_CONFIG[a].danger)
  const closing = actions.filter((a) => ACTION_CONFIG[a].danger)

  return (
    <section aria-labelledby="actions-title" className="flex flex-col gap-2 border-t border-line pt-4">
      <h2 id="actions-title" className="m-0 mb-1 text-[11px] font-semibold uppercase tracking-wider text-muted">
        Acciones
      </h2>
      {main.map((a, i) => (
        <Button
          key={a}
          variant={i === 0 ? 'primary' : 'secondary'}
          className="w-full !justify-start"
          onClick={() => setActive(a)}
        >
          <Icon name={ACTION_ICON[a]} size={16} strokeWidth={2} />
          {ACTION_CONFIG[a].title}
        </Button>
      ))}
      {closing.length > 0 && (
        <div className="mt-2 flex flex-col gap-2 border-t border-dashed border-line pt-3">
          {closing.map((a) => (
            <Button key={a} variant="danger-outline" className="w-full !justify-start" onClick={() => setActive(a)}>
              <Icon name={ACTION_ICON[a]} size={16} strokeWidth={2} />
              {ACTION_CONFIG[a].title}
            </Button>
          ))}
        </div>
      )}
      {active && (
        <ActionModal
          action={active}
          target={request}
          // Cancelar ofrece «Deshacer» (volver al estado actual); restaurar vuelve al que tenía al cancelarse.
          restoreTo={stateToStatus(active === 'restore' ? (lastCancellation(request)?.previous ?? request.state) : request.state)}
          onClose={() => setActive(null)}
        />
      )}
    </section>
  )
}
