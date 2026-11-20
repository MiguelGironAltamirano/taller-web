import { QueryClient } from '@tanstack/react-query'
import { ApiError } from '../shared/api/client'

/**
 * Los errores 4xx son definitivos (permiso, validación, no encontrado): no se reintentan.
 *
 * `networkMode: 'always'`: sin conexión, TanStack Query pausa por defecto las consultas y las acciones, y la pantalla
 * se queda cargando o con el botón deshabilitado sin decir nada. Así fallan y la interfaz explica «No pudimos conectar
 * con el servidor…» (guía de mensajes de error).
 */
export function createQueryClient() {
  return new QueryClient({
    defaultOptions: {
      queries: {
        staleTime: 30_000,
        refetchOnWindowFocus: false,
        networkMode: 'always',
        retry: (count, error) => !(error instanceof ApiError && error.status < 500) && count < 2,
      },
      mutations: { networkMode: 'always' },
    },
  })
}
