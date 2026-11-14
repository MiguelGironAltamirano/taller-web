import { useQuery } from '@tanstack/react-query'
import { apiGet } from '../../shared/api/client'
import { dashboardKey } from '../../shared/api/keys'
import type { DashboardIndicators, DashboardRange, TeamMemberLoad, Technician } from '../../shared/api/types'

export function useIndicators(range: DashboardRange, areaId: number | null) {
  return useQuery({
    queryKey: [...dashboardKey, 'indicators', range, areaId],
    queryFn: () => apiGet<DashboardIndicators>('/dashboard/indicators', { range, areaId }),
    placeholderData: (previous) => previous,
  })
}

export function useTeam() {
  return useQuery({
    queryKey: [...dashboardKey, 'team'],
    queryFn: async (): Promise<TeamMemberLoad[]> => {
      const technicians = await apiGet<Technician[]>('/users/technicians')
      return technicians.map((t) => ({
        id: t.id,
        fullName: t.fullName,
        assigned: t.activeRequests ?? 0,
        inProgress: 0,
        overdue: 0,
        capacity: 6,
      }))
    },
  })
}
