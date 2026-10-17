import { useCallback, useMemo } from 'react'
import { useSearchParams } from 'react-router'
import type { ListFilters, ListSort, ListView } from '../lib/filters'

const VIEWS: ListView[] = ['all', 'venc', 'open', 'confirm', 'critsa']
const SORTS: ListSort[] = ['due', 'new', 'prio']

export type ListParams = {
  filters: ListFilters
  mode: 'table' | 'kanban'
  density: 'comfy' | 'compact'
  pageSize: number
  page: number
}

/**
 * Estado del listado guardado en la URL (`?view=venc&status=Resuelta`): los enlaces del Inicio y del
 * dashboard abren la bandeja ya filtrada y cualquier vista se puede compartir.
 */
export function useListParams() {
  const [params, setParams] = useSearchParams()

  const state = useMemo<ListParams>(() => {
    const view = params.get('view') as ListView
    const sort = params.get('sort') as ListSort
    const size = Number(params.get('size'))
    return {
      filters: {
        q: params.get('q') ?? '',
        category: params.get('cat') ?? '',
        priority: params.get('prio') ?? '',
        technician: params.get('tech') ?? '',
        status: params.get('status') ?? '',
        view: VIEWS.includes(view) ? view : 'all',
        sort: SORTS.includes(sort) ? sort : 'due',
      },
      mode: params.get('mode') === 'kanban' ? 'kanban' : 'table',
      density: params.get('dens') === 'compact' ? 'compact' : 'comfy',
      pageSize: [10, 25, 50].includes(size) ? size : 10,
      page: Math.max(0, Number(params.get('page')) || 0),
    }
  }, [params])

  const set = useCallback(
    (entries: Record<string, string | null>, resetPage = true) => {
      setParams(
        (prev) => {
          const next = new URLSearchParams(prev)
          for (const [k, v] of Object.entries(entries)) {
            if (v === null || v === '' || v === 'all') next.delete(k)
            else next.set(k, v)
          }
          if (resetPage) next.delete('page')
          return next
        },
        { replace: true },
      )
    },
    [setParams],
  )

  const setFilters = useCallback(
    (patch: Partial<ListFilters>) => {
      const map: Record<string, string> = {
        q: 'q',
        category: 'cat',
        priority: 'prio',
        technician: 'tech',
        status: 'status',
        view: 'view',
        sort: 'sort',
      }
      set(Object.fromEntries(Object.entries(patch).map(([k, v]) => [map[k], v as string])))
    },
    [set],
  )

  const clearFilters = useCallback(() => set({ q: null, cat: null, prio: null, tech: null, status: null, view: null }), [set])

  return {
    ...state,
    setFilters,
    clearFilters,
    setMode: (mode: 'table' | 'kanban') => set({ mode: mode === 'kanban' ? 'kanban' : null }, false),
    setDensity: (d: 'comfy' | 'compact') => set({ dens: d === 'compact' ? 'compact' : null }, false),
    setPage: (page: number) => set({ page: page ? String(page) : null }, false),
    setPageSize: (size: number) => set({ size: size === 10 ? null : String(size) }),
  }
}
