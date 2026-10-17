import { useEffect, useMemo, useRef, useState } from 'react'
import { createPortal } from 'react-dom'
import { cn } from '../../shared/lib/cn'
import { normalizeText } from '../../shared/lib/format'
import { Icon, type IconName } from '../../shared/ui'

export type PaletteItem = { label: string; hint: string; icon: IconName; run: () => void }

type Props = {
  items: PaletteItem[]
  /** Resultados de solicitudes según el texto escrito (ya filtrados por el llamador). */
  searchRequests: (query: string) => PaletteItem[]
  onClose: () => void
}

/** Paleta de comandos (Ctrl+K): salta a una solicitud o ejecuta una acción. */
export function CommandPalette({ items, searchRequests, onClose }: Props) {
  const [query, setQuery] = useState('')
  const [active, setActive] = useState(0)
  const inputRef = useRef<HTMLInputElement>(null)

  useEffect(() => inputRef.current?.focus(), [])

  const results = useMemo(() => {
    const q = normalizeText(query.trim())
    const commands = q ? items.filter((i) => normalizeText(i.label).includes(q)) : items
    return q ? [...searchRequests(query.trim()), ...commands] : commands
  }, [query, items, searchRequests])

  const run = (item: PaletteItem | undefined) => {
    if (!item) return
    onClose()
    item.run()
  }

  const onKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Escape') onClose()
    else if (e.key === 'ArrowDown') {
      e.preventDefault()
      setActive((a) => Math.min(results.length - 1, a + 1))
    } else if (e.key === 'ArrowUp') {
      e.preventDefault()
      setActive((a) => Math.max(0, a - 1))
    } else if (e.key === 'Enter') {
      e.preventDefault()
      run(results[active])
    }
  }

  return createPortal(
    <>
      <div className="fixed inset-0 z-[60] bg-black/45" onClick={onClose} aria-hidden="true" />
      <div
        role="dialog"
        aria-modal="true"
        aria-label="Paleta de comandos"
        onKeyDown={onKeyDown}
        className="fixed left-1/2 top-[60px] z-[61] w-[min(560px,92%)] -translate-x-1/2 animate-pop overflow-hidden rounded-xl border border-line bg-surface shadow-modal md:top-[90px]"
      >
        <input
          ref={inputRef}
          value={query}
          onChange={(e) => {
            setQuery(e.target.value)
            setActive(0)
          }}
          role="combobox"
          aria-expanded="true"
          aria-controls="palette-list"
          aria-label="Buscar"
          placeholder="Busca por código, texto o acción"
          className="h-12 w-full border-0 border-b border-line bg-transparent px-4 text-[15px] outline-none"
        />
        <div id="palette-list" role="listbox" className="max-h-80 overflow-auto p-1.5">
          {results.length === 0 && <div className="p-5 text-center text-muted">Sin resultados</div>}
          {results.map((item, i) => (
            <button
              key={`${item.label}-${i}`}
              type="button"
              role="option"
              aria-selected={i === active}
              onMouseEnter={() => setActive(i)}
              onClick={() => run(item)}
              className={cn(
                'flex min-h-10 w-full items-center gap-2.5 rounded-lg border-0 px-2.5 text-left text-fg',
                i === active ? 'bg-surface2' : 'bg-transparent',
              )}
            >
              <Icon name={item.icon} size={16} className="text-muted" />
              <span className="flex-1">{item.label}</span>
              <span className="text-xs text-muted">{item.hint}</span>
            </button>
          ))}
        </div>
      </div>
    </>,
    document.body,
  )
}
