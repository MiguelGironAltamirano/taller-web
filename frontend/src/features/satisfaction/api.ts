import { useMutation, useQueryClient } from '@tanstack/react-query'
import { apiPost } from '../../shared/api/client'
import { notificationsKey, requestsKey } from '../../shared/api/keys'
import type { RateBody, RequestDetail } from '../../shared/api/types'

/** Máximo del comentario de la calificación (contrato: 1000 caracteres). */
export const RATING_COMMENT_MAX = 1000

function useRefresh() {
  const client = useQueryClient()
  return () => {
    client.invalidateQueries({ queryKey: requestsKey })
    client.invalidateQueries({ queryKey: notificationsKey })
  }
}

const postRating = (code: string, rating: RateBody) => apiPost<RequestDetail>(`/requests/${code}/rating`, rating)

export type ConfirmResult = { rated: boolean }

/**
 * Confirma la solución: es la transición Resuelta → Cerrada del contrato, que no necesita calificación (spec §6).
 * Si el solicitante calificó, la calificación se guarda después; que falle no deshace el cierre y puede reintentarse
 * desde la tarjeta de la solicitud cerrada.
 */
export function useConfirmSolution(code: string) {
  const refresh = useRefresh()
  return useMutation({
    mutationFn: async (rating: RateBody | null): Promise<ConfirmResult> => {
      await apiPost<RequestDetail>(`/requests/${code}/transitions`, { to: 'CERRADA' })
      if (!rating) return { rated: false }
      try {
        await postRating(code, rating)
        return { rated: true }
      } catch {
        return { rated: false }
      }
    },
    onSuccess: refresh,
  })
}

/** Califica una solicitud ya cerrada por confirmación (opcional, una sola vez). */
export function useRateRequest(code: string) {
  const refresh = useRefresh()
  return useMutation({ mutationFn: (rating: RateBody) => postRating(code, rating), onSuccess: refresh })
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
