import type { ReactNode } from 'react'

export type TimelineItem = { id: string | number; title: ReactNode; body?: ReactNode; meta?: ReactNode }

/** Línea de tiempo vertical (historial de cambios de estado). */
export function Timeline({ items }: { items: TimelineItem[] }) {
  return (
    <ol className="m-0 flex list-none flex-col p-0">
      {items.map((it) => (
        <li key={it.id} className="flex gap-3">
          <div className="flex flex-col items-center">
            <span className="mt-[5px] size-2.5 rounded-full bg-primary" />
            <span className="w-0.5 flex-1 bg-line" />
          </div>
          <div className="pb-4">
            <div className="text-[13px] font-semibold">{it.title}</div>
            {it.body && <div className="mt-0.5 text-[13px]">{it.body}</div>}
            {it.meta && <div className="mt-0.5 text-xs text-muted">{it.meta}</div>}
          </div>
        </li>
      ))}
    </ol>
  )
}
