import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { apiGet, apiPost } from '../../shared/api/client'
import { requestDetailKey, requestListKey, requestsKey, notificationsKey } from '../../shared/api/keys'
import { toRequestDetail, toRequestRow } from '../../shared/api/mappers'
import type { CreateRequestBody, Page, RequestDetail, RequestSummary } from '../../shared/api/types'

/**
 * Bandeja del usuario. El backend ya filtra por rol (spec §5); el listado completo se pide una vez
 * para poder mostrar contadores por estado y filtrar sin esperas. Los filtros avanzados del servidor
 * quedan por definir en `docs/api/` (plan de Sevan, semanas 10–12).
 */
export function useRequestRows() {
  return useQuery({
    queryKey: requestListKey,
    queryFn: () => apiGet<Page<RequestSummary>>('/requests', { size: 500 }),
    select: (page) => page.items.map(toRequestRow),
  })
}

export function useRequestDetail(code: string | undefined) {
  return useQuery({
    queryKey: requestDetailKey(code ?? ''),
    queryFn: () => apiGet<RequestDetail>(`/requests/${code}`),
    select: toRequestDetail,
    enabled: Boolean(code),
    retry: false,
  })
}

export function useCreateRequest() {
  const client = useQueryClient()
  return useMutation({
    mutationFn: (body: CreateRequestBody) => apiPost<RequestDetail>('/requests', body),
    onSuccess: () => {
      client.invalidateQueries({ queryKey: requestsKey })
      client.invalidateQueries({ queryKey: notificationsKey })
    },
  })
}
