import { useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router'
import { useCatalogs } from '../../../shared/api/catalogs'
import { useDebouncedValue } from '../../../shared/hooks/useDebouncedValue'
import { useNow } from '../../../shared/hooks/useNow'
import { LIST_TITLES } from '../../../shared/domain'
import { downloadText } from '../../../shared/lib/download'
import { Button, Card, EmptyState, Icon, PageHeader, Pagination, SkeletonRows, useToast } from '../../../shared/ui'
import { useSessionUser } from '../../auth'
import { KanbanBoard, useTechnicians } from '../../workflow'
import { fetchAllRequestRows, useRequestCounts, useRequestPage } from '../api'
import { FilterChips } from '../components/FilterChips'
import { ListToolbar, type FilterOption } from '../components/ListToolbar'
import { RequestCards, RequestTable } from '../components/RequestTable'
import { StatusStrip } from '../components/StatusStrip'
import { toCsv, toServerQuery, toStripCounts } from '../lib/filters'
import { withSla } from '../lib/view'
import { useListParams } from './useListParams'
import { useIsMobile } from '../../../shared/hooks/useMediaQuery'

/** El tablero muestra una sola página grande; si hay más, se invita a acotar con filtros. */
const KANBAN_SIZE = 100
const CRITICAL = 'Crítica'

const labelOf = (options: FilterOption[], value: string) => options.find((o) => o.value === value)?.label

/**
 * "Mis solicitudes" / bandeja del técnico / bandeja del área / todas las solicitudes, según el rol.
 * Filtros, orden y paginación se resuelven en el servidor (`GET /requests`); los contadores vienen de
 * `GET /requests/counts`.
 */
export function RequestListPage() {
  const user = useSessionUser()
  const navigate = useNavigate()
  const toast = useToast()
  const now = useNow()
  const isMobile = useIsMobile()
  const { data: catalogs } = useCatalogs()
  const p = useListParams()
  const technicians = useTechnicians(user.role === 'supervisor' ? (user.areaId ?? undefined) : undefined)
  const q = useDebouncedValue(p.filters.q, 300)
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
  const criticalId = catalogs?.priorities.find((pr) => pr.name === CRITICAL)?.id

  const canKanban = user.role !== 'solicitante'
  const kanban = canKanban && p.mode === 'kanban'
  const plan = useMemo(() => toServerQuery({ ...p.filters, q }, criticalId), [p.filters, q, criticalId])
  const query = useMemo(
    () =>
      plan.kind === 'query'
        ? { ...plan.query, page: kanban ? 0 : p.page, size: kanban ? KANBAN_SIZE : p.pageSize }
        : undefined,
    [plan, kanban, p.page, p.pageSize],
  )
  const list = useRequestPage(query)
  const counts = useRequestCounts()

  const rows = useMemo(
    () => (plan.kind === 'query' ? (list.data?.items ?? []) : []).map((r) => withSla(r, now)),
    [plan.kind, list.data, now],
  )
  const total = plan.kind === 'query' ? (list.data?.total ?? 0) : 0
  const isLoading = plan.kind === 'pending' || (plan.kind === 'query' && list.isLoading)
  const isError = plan.kind === 'query' && list.isError
  const strip = useMemo(() => toStripCounts(counts.data), [counts.data])

  // Una página que quedó fuera de rango (p. ej. tras cerrar solicitudes) vuelve a la última que existe.
  const totalPages = list.data?.totalPages ?? 0
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
      const all = await fetchAllRequestRows(plan.query)
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
      <StatusStrip counts={strip} value={p.filters.status} onChange={(status) => p.setFilters({ status })} />
      <ListToolbar
        filters={p.filters}
        categories={categoryOptions}
        priorities={priorityOptions}
        technicians={technicianOptions}
        canKanban={canKanban}
        mode={p.mode}
        density={p.density}
        onChange={p.setFilters}
        onMode={p.setMode}
        onDensity={p.setDensity}
      />
      <FilterChips
        filters={p.filters}
        labels={{
          category: labelOf(categoryOptions, p.filters.category),
          priority: labelOf(priorityOptions, p.filters.priority),
          technician: technicianOptions ? labelOf(technicianOptions, p.filters.technician) : undefined,
        }}
        onChange={p.setFilters}
        onClear={p.clearFilters}
      />

      <Card className="overflow-hidden" aria-busy={list.isFetching}>
        {isLoading && <SkeletonRows count={5} rowClass={p.density === 'comfy' ? 'h-12' : 'h-10'} />}
        {isError && (
          <EmptyState icon="file" title="No pudimos cargar las solicitudes" description="Inténtalo de nuevo en unos momentos." />
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
            <KanbanBoard items={rows} role={user.role} onOpen={open} />
          </>
        )}
        {!isLoading && total > 0 && !kanban && (
          <>
            {isMobile ? (
              <RequestCards rows={rows} onOpen={open} />
            ) : (
              <RequestTable rows={rows} compact={p.density === 'compact'} onOpen={open} />
            )}
            <Pagination page={p.page} pageSize={p.pageSize} total={total} onPage={p.setPage} onPageSize={p.setPageSize} />
          </>
        )}
      </Card>
    </>
  )
}
