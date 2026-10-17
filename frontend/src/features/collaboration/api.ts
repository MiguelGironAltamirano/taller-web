import { useMutation, useQueryClient } from '@tanstack/react-query'
import { apiPost } from '../../shared/api/client'
import { notificationsKey, requestsKey } from '../../shared/api/keys'
import type { CreateCommentBody, RequestDetail } from '../../shared/api/types'

export function useAddComment(code: string) {
  const client = useQueryClient()
  return useMutation({
    mutationFn: (body: CreateCommentBody) => apiPost<RequestDetail>(`/requests/${code}/comments`, body),
    onSuccess: () => {
      client.invalidateQueries({ queryKey: requestsKey })
      client.invalidateQueries({ queryKey: notificationsKey })
    },
  })
}
