import { QueryClient } from '@tanstack/react-query'
import { ApiError } from '../shared/api/client'

/** Los errores 4xx son definitivos (permiso, validación, no encontrado): no se reintentan. */
export function createQueryClient() {
  return new QueryClient({
    defaultOptions: {
      queries: {
        staleTime: 30_000,
        refetchOnWindowFocus: false,
        retry: (count, error) => !(error instanceof ApiError && error.status < 500) && count < 2,
      },
    },
  })
}
