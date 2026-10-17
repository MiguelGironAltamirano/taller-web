import { useQuery } from '@tanstack/react-query'
import { apiGet } from './client'
import type { Catalogs } from './types'

export const catalogsKey = ['catalogs'] as const

/** Catálogos de consulta (áreas, categorías, prioridades, ubicaciones); casi no cambian. */
export function useCatalogs() {
  return useQuery({
    queryKey: catalogsKey,
    queryFn: () => apiGet<Catalogs>('/catalogs'),
    staleTime: 5 * 60_000,
  })
}

export type LocationTree = { id: number; name: string; pavilions: { id: number; name: string; rooms: { id: number; name: string }[] }[] }

/** Arma el árbol Facultad → Pabellón → Ambiente (spec §5.1) a partir de la lista plana. */
export function buildLocationTree(locations: Catalogs['locations']): LocationTree[] {
  return locations
    .filter((l) => l.kind === 'FACULTAD')
    .map((f) => ({
      id: f.id,
      name: f.name,
      pavilions: locations
        .filter((p) => p.parentId === f.id)
        .map((p) => ({
          id: p.id,
          name: p.name,
          rooms: locations.filter((r) => r.parentId === p.id).map((r) => ({ id: r.id, name: r.name })),
        })),
    }))
}
