import { Button, Icon } from '../../shared/ui'
import type { TriageSuggestion } from './lib/suggest'

type Props = { suggestion: TriageSuggestion; onAccept: () => void; onIgnore: () => void }

/** Aviso "Sugerimos categoría X y prioridad Y": el solicitante puede aceptarlo o ignorarlo. */
export function SuggestionBanner({ suggestion, onAccept, onIgnore }: Props) {
  return (
    <div
      role="status"
      className="flex flex-wrap items-center gap-2.5 rounded-[10px] bg-info-bg px-3 py-2.5 font-normal text-info-fg"
    >
      <Icon name="sparkles" className="shrink-0" />
      <span className="min-w-[180px] flex-1">
        Sugerimos categoría <b>{suggestion.category}</b> y prioridad <b>{suggestion.priority}</b> según tu descripción.
      </span>
      <Button variant="primary" size="sm" onClick={onAccept} className="h-[30px] px-3">
        Aceptar
      </Button>
      <button
        type="button"
        onClick={onIgnore}
        className="h-[30px] rounded-md border border-current bg-transparent px-3 text-inherit"
      >
        Ignorar
      </button>
    </div>
  )
}
