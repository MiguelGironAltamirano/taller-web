import http from 'k6/http'
import { check } from 'k6'
import { API, pick } from './config.js'
import { authHeaders } from './session.js'

/** Filters people really use on the inbox (the same parameters the SPA sends; contrato-v1.md «Listado»). */
const STATUS_FILTERS = [null, null, null, 'REGISTRADA', 'EN_EVALUACION,ASIGNADA', 'EN_ATENCION', 'RESUELTA', 'CERRADA']
const SEARCH_TERMS = [null, null, null, 'proyector', 'foco', 'caño', 'internet', 'aula', 'SOL-2025-0001']
const SORTS = ['newest', 'newest', 'due', 'priority']

/** A query of the inbox as the interface builds it: 10 per page, a status, a search or an order, the first pages. */
export function randomListQuery() {
  const params = [`page=${pick([0, 0, 0, 1, 2])}`, 'size=10', `sort=${pick(SORTS)}`]
  const status = pick(STATUS_FILTERS)
  const q = pick(SEARCH_TERMS)
  if (status) params.push(`status=${status}`)
  if (q) params.push(`q=${encodeURIComponent(q)}`)
  return params.join('&')
}

/** One page of the inbox; `tags` carry the scenario/stage so each one gets its own thresholds. */
export function listRequests(token, tags = {}) {
  const res = http.get(`${API}/requests?${randomListQuery()}`, {
    ...authHeaders(token),
    tags: { endpoint: 'list', name: 'GET /requests', ...tags },
  })
  check(res, {
    'list 200': (r) => r.status === 200,
    'list is a page': (r) => r.status === 200 && Array.isArray(r.json('items')) && r.json('size') === 10,
  })
  return res
}

/** The counters of the inbox (the status strip), loaded together with the list when the inbox opens. */
export function requestCounts(token, tags = {}) {
  const res = http.get(`${API}/requests/counts`, {
    ...authHeaders(token),
    tags: { endpoint: 'counts', name: 'GET /requests/counts', ...tags },
  })
  check(res, { 'counts 200': (r) => r.status === 200 && typeof r.json('total') === 'number' })
  return res
}
