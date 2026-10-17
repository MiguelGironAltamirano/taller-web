import { Button } from '../atoms/Button'

type PaginationProps = {
  page: number
  pageSize: number
  total: number
  onPage: (page: number) => void
  onPageSize: (size: number) => void
}

const PAGE_SIZES = [10, 25, 50] as const

export function Pagination({ page, pageSize, total, onPage, onPageSize }: PaginationProps) {
  const pages = Math.max(1, Math.ceil(total / pageSize))
  const current = Math.min(page, pages - 1)
  const from = total ? current * pageSize + 1 : 0
  const to = Math.min(total, current * pageSize + pageSize)

  return (
    <div className="flex flex-wrap items-center justify-between gap-2.5 px-4 py-2 text-[13px] text-muted">
      <label className="flex items-center gap-1.5">
        Filas
        <select
          value={pageSize}
          onChange={(e) => onPageSize(Number(e.target.value))}
          className="h-[30px] rounded-md border border-line bg-surface"
        >
          {PAGE_SIZES.map((s) => (
            <option key={s} value={s}>
              {s}
            </option>
          ))}
        </select>
      </label>
      <span>{total ? `${from}–${to} de ${total}` : '0 resultados'}</span>
      <span className="flex gap-1.5">
        <Button size="sm" disabled={current === 0} onClick={() => onPage(current - 1)} className="px-3">
          Anterior
        </Button>
        <Button size="sm" disabled={current >= pages - 1} onClick={() => onPage(current + 1)} className="px-3">
          Siguiente
        </Button>
      </span>
    </div>
  )
}
