import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { apiGet, apiPost, type QueryParams } from '../../shared/api/client'
import {
  notificationsKey,
  requestCountsKey,
  requestDetailKey,
  requestListKey,
  requestPageKey,
  requestsKey,
} from '../../shared/api/keys'
import { statusToState, toRequestDetail, toRequestRow } from '../../shared/api/mappers'
import {
  MAX_REQUEST_PAGE_SIZE,
  type CreateRequestBody,
  type Page,
  type RequestCounts,
  type RequestDetail,
  type RequestListQuery,
  type RequestSummary,
} from '../../shared/api/types'
import { actionsFromTransitions } from '../workflow'

/** `status` viaja como lista separada por comas (el servidor también acepta el parámetro repetido). */
export function toQueryParams(query: RequestListQuery): QueryParams {
  const { status, ...rest } = query
  return { ...rest, status: status?.length ? status.join(',') : undefined }
}

const fetchPage = (query: RequestListQuery) => apiGet<Page<RequestSummary>>('/requests', toQueryParams(query))

/**
 * Todo lo visible para el usuario (hasta 500), para las vistas que agregan sobre el conjunto completo:
 * Inicio, calendario, reportes y el panel "Requieren tu atención". La bandeja paginada usa `useRequestPage`.
 */
export function useRequestRows() {
  return useQuery({
    queryKey: requestListKey,
    queryFn: () => fetchPage({ size: MAX_REQUEST_PAGE_SIZE }),
    select: (page) => page.items.map(toRequestRow),
    refetchInterval: 30_000,
  })
}

/**
 * Una página de la bandeja filtrada, ordenada y paginada en el servidor. Sin `query` no consulta (la combinación
 * de filtros no puede devolver nada o falta un dato del catálogo). Mantiene la página anterior mientras llega la
 * siguiente para que la tabla no parpadee.
 */
export function useRequestPage(query: RequestListQuery | undefined) {
  return useQuery({
    queryKey: requestPageKey(query ?? {}),
    queryFn: () => fetchPage(query!),
    enabled: query !== undefined,
    placeholderData: keepPreviousData,
    select: (page) => ({ ...page, items: page.items.map(toRequestRow) }),
  })
}

/** Exportación: la primera página de 500 con los mismos filtros; `total` dice si quedó algo fuera. */
export async function fetchAllRequestRows(query: RequestListQuery) {
  const page = await fetchPage({ ...query, page: 0, size: MAX_REQUEST_PAGE_SIZE })
  return { rows: page.items.map(toRequestRow), total: page.total }
}

/** Contadores de la bandeja (total, vencidas y por estado) sobre todo lo visible para el usuario. */
export function useRequestCounts() {
  return useQuery({
    queryKey: requestCountsKey,
    queryFn: () => apiGet<RequestCounts>('/requests/counts'),
  })
}

export function useRequestDetail(code: string | undefined) {
  return useQuery({
    queryKey: requestDetailKey(code ?? ''),
    queryFn: () => apiGet<RequestDetail>(`/requests/${code}`),
    select: (detail) => {
      const view = toRequestDetail(detail)
      // El contrato solo define `allowedTransitions`: si el servidor no envía las acciones de la interfaz
      // (`allowedActions`, extensión), se derivan de las transiciones que sí permite.
      if (detail.allowedActions) return view
      const allowed = detail.allowedTransitions.map(statusToState)
      return { ...view, allowedActions: actionsFromTransitions(view.state, allowed) }
    },
    enabled: Boolean(code),
    retry: false,
  })
}

export function useCreateRequest() {
  const client = useQueryClient()
  return useMutation({
    mutationFn: (body: CreateRequestBody) => apiPost<RequestDetail>('/requests', body),
    onSuccess: () => {
      client.invalidateQueries({ queryKey: requestsKey })
      client.invalidateQueries({ queryKey: notificationsKey })
    },
  })
}
