import { useMemo } from 'react'
import { useNavigate } from 'react-router'
import { useCatalogs } from '../../../shared/api/catalogs'
import { useNow } from '../../../shared/hooks/useNow'
import { LIST_TITLES } from '../../../shared/domain'
import { downloadText } from '../../../shared/lib/download'
import { Button, Card, EmptyState, Icon, PageHeader, Pagination, SkeletonRows, useToast } from '../../../shared/ui'
import { useSessionUser } from '../../auth'
import { KanbanBoard, useTechnicians } from '../../workflow'
import { useRequestRows } from '../api'
import { FilterChips } from '../components/FilterChips'
import { ListToolbar } from '../components/ListToolbar'
import { RequestCards, RequestTable } from '../components/RequestTable'
import { StatusStrip } from '../components/StatusStrip'
import { countByState, filterRows, sortRows, toCsv } from '../lib/filters'
import { withSla } from '../lib/view'
import { useListParams } from './useListParams'
import { useIsMobile } from '../../../shared/hooks/useMediaQuery'

/** "Mis solicitudes" / bandeja del técnico / bandeja del área / todas las solicitudes, según el rol. */
export function RequestListPage() {
  const user = useSessionUser()
  const navigate = useNavigate()
  const toast = useToast()
  const now = useNow()
  const isMobile = useIsMobile()
  const { data: catalogs } = useCatalogs()
  const { data: rows, isLoading, isError } = useRequestRows()
  const p = useListParams()
  const technicians = useTechnicians(user.role === 'supervisor' ? (user.areaId ?? undefined) : undefined)

  const all = useMemo(() => (rows ?? []).map((r) => withSla(r, now)), [rows, now])
  const counts = useMemo(() => countByState(all), [all])
  const filtered = useMemo(() => sortRows(filterRows(all, p.filters), p.filters.sort), [all, p.filters])

  const canKanban = user.role !== 'solicitante'
  const kanban = canKanban && p.mode === 'kanban'
  const pageSize = p.pageSize
  const page = Math.min(p.page, Math.max(0, Math.ceil(filtered.length / pageSize) - 1))
  const pageRows = filtered.slice(page * pageSize, page * pageSize + pageSize)
  const open = (code: string) => navigate(`/solicitudes/${code}`)

  const exportCsv = () => {
    downloadText(`solicitudes-${new Date().toISOString().slice(0, 10)}.csv`, toCsv(filtered))
    toast(`Exportación CSV generada (${filtered.length} filas)`)
  }

  return (
    <>
      <PageHeader
        title={LIST_TITLES[user.role]}
        subtitle={`${filtered.length} de ${all.length} solicitudes`}
        actions={
          <>
            <Button onClick={exportCsv} disabled={filtered.length === 0}>
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
      <StatusStrip counts={counts} value={p.filters.status} onChange={(status) => p.setFilters({ status })} />
      <ListToolbar
        filters={p.filters}
        categories={catalogs?.categories.map((c) => c.name) ?? []}
        technicians={user.role === 'supervisor' ? (technicians.data?.map((t) => t.fullName) ?? []) : null}
        canKanban={canKanban}
        mode={p.mode}
        density={p.density}
        onChange={p.setFilters}
        onMode={p.setMode}
        onDensity={p.setDensity}
      />
      <FilterChips filters={p.filters} onChange={p.setFilters} onClear={p.clearFilters} />

      <Card className="overflow-hidden">
        {isLoading && <SkeletonRows count={5} rowClass={p.density === 'comfy' ? 'h-12' : 'h-10'} />}
        {isError && (
          <EmptyState icon="file" title="No pudimos cargar las solicitudes" description="Inténtalo de nuevo en unos momentos." />
        )}
        {!isLoading && !isError && filtered.length === 0 && (
          <EmptyState
            title="Sin resultados"
            description="Ninguna solicitud coincide con los filtros."
            action={<Button onClick={p.clearFilters}>Limpiar filtros</Button>}
          />
        )}
        {!isLoading && filtered.length > 0 && kanban && <KanbanBoard items={filtered} role={user.role} onOpen={open} />}
        {!isLoading && filtered.length > 0 && !kanban && (
          <>
            {isMobile ? (
              <RequestCards rows={pageRows} onOpen={open} />
            ) : (
              <RequestTable rows={pageRows} compact={p.density === 'compact'} onOpen={open} />
            )}
            <Pagination page={page} pageSize={pageSize} total={filtered.length} onPage={p.setPage} onPageSize={p.setPageSize} />
          </>
        )}
      </Card>
    </>
  )
}
