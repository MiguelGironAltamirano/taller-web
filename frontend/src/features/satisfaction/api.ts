import { useMutation, useQueryClient } from '@tanstack/react-query'
import { apiPost } from '../../shared/api/client'
import { notificationsKey, requestsKey } from '../../shared/api/keys'
import type { RateBody, RequestDetail } from '../../shared/api/types'

function useRefresh() {
  const client = useQueryClient()
  return () => {
    client.invalidateQueries({ queryKey: requestsKey })
    client.invalidateQueries({ queryKey: notificationsKey })
  }
}

/** Confirma la solución y cierra la solicitud con su calificación (Resuelta → Cerrada). */
export function useRate(code: string) {
  const refresh = useRefresh()
  return useMutation({
    mutationFn: (body: RateBody) => apiPost<RequestDetail>(`/requests/${code}/rating`, body),
    onSuccess: refresh,
  })
}

/** Rechaza la solución: la solicitud vuelve a En atención y exige un motivo (Resuelta → En atención). */
export function useRejectSolution(code: string) {
  const refresh = useRefresh()
  return useMutation({
    mutationFn: (reason: string) =>
      apiPost<RequestDetail>(`/requests/${code}/transitions`, { to: 'EN_ATENCION', reason }),
    onSuccess: refresh,
  })
}
