import { ErrorState } from '../ui'
import { errorMessage } from './errors'

type FailedQuery = { error: unknown; refetch: () => unknown; isFetching: boolean }

/**
 * Estado de error de una consulta: dice qué no se pudo cargar, por qué (con `errorMessage`) y ofrece «Reintentar».
 * Se muestra en lugar del contenido, nunca junto a un «Sin resultados» que haría creer que no hay datos.
 */
export function QueryErrorState({ what, query, compact }: { what: string; query: FailedQuery; compact?: boolean }) {
  return (
    <ErrorState
      title={`No pudimos cargar ${what}`}
      description={errorMessage(query.error, 'Vuelve a intentarlo en unos segundos.')}
      onRetry={() => void query.refetch()}
      retrying={query.isFetching}
      compact={compact}
    />
  )
}
