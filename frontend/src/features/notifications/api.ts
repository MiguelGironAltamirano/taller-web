import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { apiGet, apiPatch, apiPost } from '../../shared/api/client'
import { toNotificationItem } from '../../shared/api/mappers'
import { notificationsKey } from '../../shared/api/keys'
import type { NotificationDto } from '../../shared/api/types'

export function useNotifications() {
  return useQuery({
    queryKey: notificationsKey,
    queryFn: () => apiGet<NotificationDto[]>('/notifications'),
    select: (items) => items.map(toNotificationItem),
    refetchInterval: 30_000,
  })
}

export function useUnreadCount(): number {
  const { data } = useNotifications()
  return data?.filter((n) => !n.read).length ?? 0
}

export function useMarkRead() {
  const client = useQueryClient()
  return useMutation({
    mutationFn: (id: number) => apiPatch(`/notifications/${id}/read`),
    onSuccess: () => client.invalidateQueries({ queryKey: notificationsKey }),
  })
}

export function useMarkAllRead() {
  const client = useQueryClient()
  return useMutation({
    mutationFn: () => apiPost('/notifications/read-all'),
    onSuccess: () => client.invalidateQueries({ queryKey: notificationsKey }),
  })
}
