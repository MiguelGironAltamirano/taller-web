import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { catalogsKey } from '../../shared/api/catalogs'
import { apiDelete, apiGet, apiPatch, apiPost, apiPut } from '../../shared/api/client'
import { adminKey } from '../../shared/api/keys'
import type { AdminUser, AdminUserBody, AppConfig, AuditEvent, StateInfo } from '../../shared/api/types'

export function useAdminUsers() {
  return useQuery({ queryKey: [...adminKey, 'users'], queryFn: () => apiGet<AdminUser[]>('/admin/users') })
}

export function useSaveUser() {
  const client = useQueryClient()
  return useMutation({
    mutationFn: ({ id, body }: { id: number | null; body: AdminUserBody }) =>
      id === null ? apiPost<AdminUser>('/admin/users', body) : apiPut<AdminUser>(`/admin/users/${id}`, body),
    onSuccess: () => client.invalidateQueries({ queryKey: adminKey }),
  })
}

export function useSetUserActive() {
  const client = useQueryClient()
  return useMutation({
    mutationFn: ({ id, active }: { id: number; active: boolean }) => apiPatch<AdminUser>(`/admin/users/${id}/active`, { active }),
    onSuccess: () => client.invalidateQueries({ queryKey: adminKey }),
  })
}

/** Cambios de catálogos: refrescan la lista compartida que usan los formularios de otras features. */
function useCatalogMutation<TVars>(fn: (vars: TVars) => Promise<unknown>) {
  const client = useQueryClient()
  return useMutation({
    mutationFn: fn,
    onSuccess: () => client.invalidateQueries({ queryKey: catalogsKey }),
  })
}

export const useCreateArea = () => useCatalogMutation((name: string) => apiPost('/admin/areas', { name }))
export const useDeleteArea = () => useCatalogMutation((id: number) => apiDelete(`/admin/areas/${id}`))
export const useCreateCategory = () =>
  useCatalogMutation((body: { name: string; areaId: number }) => apiPost('/admin/categories', body))
export const useDeleteCategory = () => useCatalogMutation((id: number) => apiDelete(`/admin/categories/${id}`))
export const useUpdatePriority = () =>
  useCatalogMutation(({ id, slaHours }: { id: number; slaHours: number }) => apiPut(`/admin/priorities/${id}`, { slaHours }))
export const useCreateLocation = () =>
  useCatalogMutation((body: { faculty: string; pavilion: string; room: string }) => apiPost('/admin/locations', body))
export const useDeleteLocation = () => useCatalogMutation((id: number) => apiDelete(`/admin/locations/${id}`))

export function useStates() {
  return useQuery({ queryKey: [...adminKey, 'states'], queryFn: () => apiGet<StateInfo[]>('/admin/states'), staleTime: Infinity })
}

export function useAudit(q: string) {
  return useQuery({
    queryKey: [...adminKey, 'audit', q],
    queryFn: () => apiGet<AuditEvent[]>('/admin/audit', { q }),
    placeholderData: (previous) => previous,
  })
}

export function useConfig() {
  return useQuery({ queryKey: [...adminKey, 'config'], queryFn: () => apiGet<AppConfig>('/admin/config') })
}

export function useSaveConfig() {
  const client = useQueryClient()
  return useMutation({
    mutationFn: (body: AppConfig) => apiPut<AppConfig>('/admin/config', body),
    onSuccess: () => client.invalidateQueries({ queryKey: adminKey }),
  })
}
