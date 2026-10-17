import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { apiGet, apiPatch, apiPost } from '../../shared/api/client'
import { notificationsKey, requestsKey } from '../../shared/api/keys'
import type { RequestDetail, Technician } from '../../shared/api/types'
import type { ApiCall } from './schemas/action'

/** Ejecuta una acción de flujo sobre una solicitud y refresca bandejas, detalle y notificaciones. */
export function useWorkflowAction(code: string) {
  const client = useQueryClient()
  return useMutation({
    mutationFn: (call: ApiCall) =>
      call.kind === 'transition'
        ? apiPost<RequestDetail>(`/requests/${code}/transitions`, call.body)
        : apiPatch<RequestDetail>(`/requests/${code}/priority`, call.body),
    onSuccess: () => {
      client.invalidateQueries({ queryKey: requestsKey })
      client.invalidateQueries({ queryKey: notificationsKey })
    },
  })
}

/** Técnicos activos del área, con su carga actual (para asignar y reasignar). */
export function useTechnicians(areaId: number | undefined) {
  return useQuery({
    queryKey: ['technicians', areaId],
    queryFn: () => apiGet<Technician[]>('/users/technicians', { areaId }),
    enabled: areaId !== undefined,
  })
}
