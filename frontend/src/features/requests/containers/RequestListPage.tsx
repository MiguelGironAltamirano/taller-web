import { useCallback, useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router'
import { useCatalogs } from '../../../shared/api/catalogs'
import { QueryErrorState } from '../../../shared/api/QueryErrorState'
import { MAX_REQUEST_PAGE_SIZE } from '../../../shared/api/types'
import { useDebouncedValue } from '../../../shared/hooks/useDebouncedValue'
import { useNow } from '../../../shared/hooks/useNow'
import { LIST_TITLES } from '../../../shared/domain'
import { downloadText } from '../../../shared/lib/download'
import { Button, Card, EmptyState, Icon, PageHeader, Pagination, SkeletonRows, useToast } from '../../../shared/ui'
import { useSessionUser } from '../../auth'
import { KanbanBoard, useTechnicians } from '../../workflow'
import { fetchAllRequestRows, useRequestCounts, useRequestPage, useRequestRows } from '../api'
import { AttentionPanel } from '../components/AttentionPanel'
import { FilterChips } from '../components/FilterChips'
import { ListToolbar, type FilterOption } from '../components/ListToolbar'
import { RequestCards, RequestTable } from '../components/RequestTable'
import { StatusStrip } from '../components/StatusStrip'
import { attentionFor, sortByUrgency, useUnseenCodes } from '../lib/attention'
import { OVERDUE_STATUS, toCsv, toServerQuery, toStripCounts, type ListFilters } from '../lib/filters'
import { withSla, type RowView } from '../lib/view'
import { useListParams } from './useListParams'
import { useIsMobile } from '../../../shared/hooks/useMediaQuery'

/** El tablero muestra una sola página grande; si hay más, se invita a acotar con filtros. */
const KANBAN_SIZE = 100
const CRITICAL = 'Crítica'

const labelOf = (options: FilterOption[] | null, value: string) => options?.find((o) => o.value === value)?.label

/**
 * "Mis solicitudes" / bandeja del técnico / bandeja del área / todas las solicitudes, según el rol.
 * Filtros, orden y paginación se resuelven en el servidor (`GET /requests`); los contadores vienen de
 * `GET /requests/counts`. La vista "Requieren mi atención" depende de las notificaciones sin leer (dato del
 * cliente): el servidor devuelve hasta 500 candidatas con los demás filtros y aquí se refinan y paginan.
 */
export function RequestListPage() {
  const user = useSessionUser()
  const navigate = useNavigate()
  const toast = useToast()
  const now = useNow()
  const isMobile = useIsMobile()
  const { data: catalogs } = useCatalogs()
  const p = useListParams()
  // El solicitante ve una fecha estimada que nunca vence (contrato, «Prioridad que ve el solicitante»): sin vista ni
  // pestaña de vencidas.
  const seesDue = user.role !== 'solicitante'
  const filters = useMemo<ListFilters>(
    () =>
      seesDue
        ? p.filters
        : {
            ...p.filters,
            view: p.filters.view === 'venc' ? 'all' : p.filters.view,
            status: p.filters.status === OVERDUE_STATUS ? '' : p.filters.status,
          },
    [seesDue, p.filters],
  )
  const technicians = useTechnicians(user.role === 'supervisor' ? (user.areaId ?? undefined) : undefined)
  const q = useDebouncedValue(filters.q, 300)
  const [exporting, setExporting] = useState(false)

  const categoryOptions = useMemo<FilterOption[]>(
    () => catalogs?.categories.map((c) => ({ value: String(c.id), label: c.name })) ?? [],
    [catalogs],
  )
  const priorityOptions = useMemo<FilterOption[]>(
    () => catalogs?.priorities.map((pr) => ({ value: String(pr.id), label: pr.name })) ?? [],
    [catalogs],
  )
  const technicianOptions = useMemo<FilterOption[] | null>(
    () => (user.role === 'supervisor' ? (technicians.data?.map((t) => ({ value: String(t.id), label: t.fullName })) ?? []) : null),
    [user.role, technicians.data],
  )
  const allAreas = useMemo<FilterOption[]>(
    () => catalogs?.areas.map((a) => ({ value: String(a.id), label: a.name })) ?? [],
    [catalogs],
  )
  // Solo el administrador elige área: los demás roles ya ven una sola (o sus propias solicitudes).
  const areaOptions = user.role === 'administrador' ? allAreas : null
  const criticalId = catalogs?.priorities.find((pr) => pr.name === CRITICAL)?.id

  // Panel "Requieren tu atención": sobre todo lo visible, sin los filtros de la bandeja.
  const unseen = useUnseenCodes()
  const attentionOf = useCallback((r: RowView) => attentionFor(user.role, r, unseen), [user.role, unseen])
  const visible = useRequestRows().data
  const attentionItems = useMemo(() => {
    const rows = (visible ?? []).map((r) => withSla(r, now)).filter((r) => attentionOf(r))
    return sortByUrgency(rows, unseen).map((row) => ({ row, attention: attentionOf(row)!, unseen: unseen.has(row.code) }))
  }, [visible, now, attentionOf, unseen])

  const canKanban = user.role !== 'solicitante'
  const kanban = canKanban && p.mode === 'kanban'
  const attentionView = filters.view === 'attention'
  const plan = useMemo(() => toServerQuery({ ...filters, q }, criticalId), [filters, q, criticalId])
  const query = useMemo(() => {
    if (plan.kind !== 'query') return undefined
    if (attentionView) return { ...plan.query, page: 0, size: MAX_REQUEST_PAGE_SIZE }
    return { ...plan.query, page: kanban ? 0 : p.page, size: kanban ? KANBAN_SIZE : p.pageSize }
  }, [plan, attentionView, kanban, p.page, p.pageSize])
  const list = useRequestPage(query)
  const counts = useRequestCounts()

  const fetched = useMemo(
    () => (plan.kind === 'query' ? (list.data?.items ?? []) : []).map((r) => withSla(r, now)),
    [plan.kind, list.data, now],
  )
  const attentionRows = useMemo(
    () => (attentionView ? sortByUrgency(fetched.filter((r) => attentionOf(r)), unseen) : []),
    [attentionView, fetched, attentionOf, unseen],
  )
  const total = attentionView ? attentionRows.length : plan.kind === 'query' ? (list.data?.total ?? 0) : 0
  const rows = useMemo(() => {
    if (!attentionView) return fetched
    return kanban ? attentionRows : attentionRows.slice(p.page * p.pageSize, (p.page + 1) * p.pageSize)
  }, [attentionView, fetched, attentionRows, kanban, p.page, p.pageSize])
  const isLoading = plan.kind === 'pending' || (plan.kind === 'query' && list.isLoading)
  const isError = plan.kind === 'query' && list.isError
  const strip = useMemo(() => toStripCounts(counts.data), [counts.data])

  // Una página que quedó fuera de rango (p. ej. tras cerrar solicitudes) vuelve a la última que existe.
  const totalPages = attentionView ? Math.ceil(total / p.pageSize) : (list.data?.totalPages ?? 0)
  const { setPage } = p
  useEffect(() => {
    if (!kanban && !list.isPlaceholderData && list.data && p.page > 0 && p.page >= totalPages) {
      setPage(Math.max(0, totalPages - 1))
    }
  }, [kanban, list.isPlaceholderData, list.data, p.page, totalPages, setPage])

  const open = (code: string) => navigate(`/solicitudes/${code}`)

  const exportCsv = async () => {
    if (plan.kind !== 'query') return
    setExporting(true)
    try {
      const all = attentionView ? { rows: attentionRows, total: attentionRows.length } : await fetchAllRequestRows(plan.query)
      const views = all.rows.map((r) => withSla(r, Date.now()))
      downloadText(`solicitudes-${new Date().toISOString().slice(0, 10)}.csv`, toCsv(views))
      toast(
        all.total > views.length
          ? `Exportación CSV generada (${views.length} de ${all.total} filas; acota los filtros para el resto)`
          : `Exportación CSV generada (${views.length} filas)`,
      )
    } catch {
      toast('No pudimos generar la exportación. Inténtalo de nuevo.')
    } finally {
      setExporting(false)
    }
  }

  return (
    <>
      <PageHeader
        icon="file"
        title={LIST_TITLES[user.role]}
        subtitle={`${total} de ${strip.total} solicitudes`}
        actions={
          <>
            <Button onClick={exportCsv} disabled={total === 0 || exporting}>
              <Icon name="download" size={16} />
              Exportar
            </Button>
            {user.role === 'solicitante' && (
              <Button variant="primary" size="lg" className="md:hidden" onClick={() => navigate('/solicitudes/nueva')}>
                Nueva solicitud
              </Button>
            )}
          </>
        }
      />
      {!attentionView && (
        <AttentionPanel
          items={attentionItems}
          onOpen={open}
          onShowAll={() => p.setFilters({ view: 'attention', status: '' })}
        />
      )}

      <Card className="overflow-hidden" aria-busy={list.isFetching}>
        <StatusStrip
          counts={strip}
          value={filters.status}
          showOverdue={seesDue}
          onChange={(status) => p.setFilters({ status })}
        />
        <div className="flex flex-col gap-2.5 border-y border-line bg-surface2/40 p-3">
          <ListToolbar
            filters={filters}
            categories={categoryOptions}
            priorities={priorityOptions}
            technicians={technicianOptions}
            areas={areaOptions}
            canKanban={canKanban}
            showDue={seesDue}
            mode={p.mode}
            density={p.density}
            onChange={p.setFilters}
            onMode={p.setMode}
            onDensity={p.setDensity}
          />
          <FilterChips
            filters={filters}
            labels={{
              category: labelOf(categoryOptions, filters.category),
              priority: labelOf(priorityOptions, filters.priority),
              technician: labelOf(technicianOptions, filters.technician),
              area: labelOf(allAreas, filters.area),
            }}
            onChange={p.setFilters}
            onClear={p.clearFilters}
          />
        </div>
        {isLoading && <SkeletonRows count={5} rowClass={p.density === 'comfy' ? 'h-12' : 'h-10'} />}
        {isError && (
          <QueryErrorState what="las solicitudes" query={list} />
        )}
        {!isLoading && !isError && total === 0 && (
          <EmptyState
            title="Sin resultados"
            description="Ninguna solicitud coincide con los filtros."
            action={<Button onClick={p.clearFilters}>Limpiar filtros</Button>}
          />
        )}
        {!isLoading && total > 0 && kanban && (
          <>
            {total > rows.length && (
              <p role="status" className="px-4 pt-3 text-[13px] text-muted">
                Se muestran {rows.length} de {total} solicitudes. Usa los filtros para acotar el tablero.
              </p>
            )}
            <KanbanBoard items={rows} role={user.role} onOpen={open} attentionOf={attentionOf} unseen={unseen} />
          </>
        )}
        {!isLoading && total > 0 && !kanban && (
          <>
            {isMobile ? (
              <RequestCards rows={rows} onOpen={open} attentionOf={attentionOf} unseen={unseen} />
            ) : (
              <RequestTable
                rows={rows}
                compact={p.density === 'compact'}
                onOpen={open}
                attentionOf={attentionOf}
                unseen={unseen}
              />
            )}
            <Pagination page={p.page} pageSize={p.pageSize} total={total} onPage={p.setPage} onPageSize={p.setPageSize} />
          </>
        )}
      </Card>
    </>
  )
}
