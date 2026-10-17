import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { apiGet, apiPost, apiUploadFile } from '../../shared/api/client'
import { commentsKey, evidencesKey, notificationsKey, requestsKey } from '../../shared/api/keys'
import type { CommentDto, CreateCommentBody, EvidenceDto } from '../../shared/api/types'

export function useComments(code: string) {
  return useQuery({
    queryKey: commentsKey(code),
    queryFn: () => apiGet<CommentDto[]>(`/requests/${code}/comments`),
  })
}

export function useEvidences(code: string) {
  return useQuery({
    queryKey: evidencesKey(code),
    queryFn: () => apiGet<EvidenceDto[]>(`/requests/${code}/evidences`),
  })
}

export function useUploadEvidence(code: string) {
  const client = useQueryClient()
  return useMutation({
    mutationFn: (file: File) => apiUploadFile<EvidenceDto>(`/requests/${code}/evidences`, file),
    onSuccess: () => {
      client.invalidateQueries({ queryKey: evidencesKey(code) })
      client.invalidateQueries({ queryKey: requestsKey })
    },
  })
}

export function useAddComment(code: string) {
  const client = useQueryClient()
  return useMutation({
    mutationFn: (body: CreateCommentBody) => apiPost<CommentDto>(`/requests/${code}/comments`, body),
    onSuccess: () => {
      client.invalidateQueries({ queryKey: commentsKey(code) })
      client.invalidateQueries({ queryKey: requestsKey })
      client.invalidateQueries({ queryKey: notificationsKey })
    },
  })
}
