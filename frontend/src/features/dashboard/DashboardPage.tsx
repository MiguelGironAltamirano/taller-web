import { useState } from 'react'
import { useNavigate } from 'react-router'
import { useCatalogs } from '../../shared/api/catalogs'
import { toRequestRow } from '../../shared/api/mappers'
import type { DashboardRange } from '../../shared/api/types'
import { useNow } from '../../shared/hooks/useNow'
import { buildCsv } from '../../shared/lib/csv'
import { downloadText } from '../../shared/lib/download'
import { cn } from '../../shared/lib/cn'
import { Button, Card, CardTitle, PageHeader, PriorityBadge, Segmented, Select, Skeleton, useToast } from '../../shared/ui'
import { useSessionUser } from '../auth'
import { slaBarColor, withSla } from '../requests'
import { useIndicators } from './api'
import { HorizontalBars, PriorityDonut, SlaGauge, Sparkline, TimelineChart } from './components/Charts'
import { kpiTone } from './lib/reports'

const RANGES = [
  { id: '7' as const, label: '7 días' },
  { id: '30' as const, label: '30 días' },
  { id: 'mes' as const, label: 'Este mes' },
]

// "Más es mejor" para cada indicador (define si la variación se pinta verde o roja).
const KPI_META = [
  { key: 'registered', label: 'Solicitudes registradas', higherIsBetter: true, suffix: '' },
  { key: 'pending', label: 'Pendientes', higherIsBetter: false, suffix: '' },
  { key: 'attended', label: 'Atendidas', higherIsBetter: true, suffix: '' },
  { key: 'resolvedPct', label: '% resueltas', higherIsBetter: true, suffix: '%' },
  { key: 'avgAttentionHours', label: 'Tiempo promedio de atención', higherIsBetter: false, suffix: ' h' },
  { key: 'overdue', label: 'Solicitudes vencidas', higherIsBetter: false, suffix: '' },
] as const

/** Dashboard con los nueve indicadores del enunciado (supervisor: su área; administrador: todas). */
export function DashboardPage() {
  const user = useSessionUser()
  const navigate = useNavigate()
  const toast = useToast()
  const now = useNow()
  const { data: catalogs } = useCatalogs()
  const [range, setRange] = useState<DashboardRange>('30')
  const [areaId, setAreaId] = useState<number | null>(null)
  const isAdmin = user.role === 'administrador'
  const { data, isLoading } = useIndicators(range, isAdmin ? areaId : null)

  const exportCsv = () => {
    if (!data) return
    const lines: (string | number)[][] = [
      ...KPI_META.map((k) => [k.label, `${data.kpis[k.key].value}${k.suffix}`]),
      ['Cumplimiento de SLA', `${data.slaCompliancePct}%`],
      ...data.byCategory.map((d) => [`Categoría: ${d.label}`, d.value]),
      ...data.byPriority.map((d) => [`Prioridad: ${d.label}`, d.value]),
      ...data.byTechnician.map((d) => [`Responsable: ${d.label}`, d.value]),
    ]
    downloadText(`dashboard-${new Date().toISOString().slice(0, 10)}.csv`, buildCsv(['Indicador', 'Valor'], lines))
    toast('Dashboard exportado (CSV)')
  }

  const title = isAdmin ? 'Dashboard global' : `Dashboard · ${catalogs?.areas.find((a) => a.id === user.areaId)?.name ?? ''}`
  const total = data?.byPriority.reduce((a, b) => a + b.value, 0) ?? 0

  return (
    <>
      <PageHeader
        title={title}
        subtitle={data ? `Últimos ${data.rangeDays} días · ${data.areaName ?? 'Todas las áreas'}` : ' '}
        actions={
          <>
            <Segmented label="Rango" value={range} onChange={setRange} items={RANGES.map((r) => ({ id: r.id, label: r.label }))} />
            {isAdmin && (
              <Select
                aria-label="Área"
                value={areaId ?? ''}
                onChange={(e) => setAreaId(e.target.value ? Number(e.target.value) : null)}
                className="h-9 w-auto max-md:h-11"
              >
                <option value="">Todas las áreas</option>
                {catalogs?.areas.map((a) => (
                  <option key={a.id} value={a.id}>
                    {a.name}
                  </option>
                ))}
              </Select>
            )}
            <Button onClick={exportCsv} disabled={!data}>
              Exportar
            </Button>
          </>
        }
      />

      {isLoading || !data ? (
        <div className="grid grid-cols-[repeat(auto-fit,minmax(200px,1fr))] gap-3" role="status" aria-label="Cargando indicadores">
          {Array.from({ length: 6 }, (_, i) => (
            <Skeleton key={i} className="h-28" />
          ))}
        </div>
      ) : (
        <>
          <div className="grid grid-cols-[repeat(auto-fit,minmax(140px,1fr))] gap-3 md:grid-cols-[repeat(auto-fit,minmax(200px,1fr))]">
            {KPI_META.map((k) => {
              const kpi = data.kpis[k.key]
              const tone = kpiTone(kpi.deltaPct, k.higherIsBetter)
              return (
                <Card key={k.key} className="p-3.5">
                  <div className="text-xs text-muted">{k.label}</div>
                  <div className="my-0.5 text-[26px] font-bold">
                    {k.key === 'avgAttentionHours' && kpi.value === 0 ? '—' : `${kpi.value}${k.suffix}`}
                  </div>
                  <div className={cn('text-xs font-semibold', tone === 'good' ? 'text-good' : 'text-err-fg')}>
                    {kpi.deltaPct > 0 ? '▲' : '▼'} {Math.abs(kpi.deltaPct)}% vs. periodo anterior
                  </div>
                  <Sparkline series={kpi.series} color={tone === 'good' ? 'var(--good)' : 'var(--bad)'} />
                </Card>
              )
            })}
          </div>

          <div className="grid grid-cols-[repeat(auto-fit,minmax(min(100%,340px),1fr))] gap-3">
            <Card className="p-4">
              <CardTitle className="text-sm">Solicitudes por categoría</CardTitle>
              <HorizontalBars data={data.byCategory} color="var(--primary)" label="Solicitudes por categoría" />
            </Card>
            <Card className="p-4">
              <CardTitle className="text-sm">Por prioridad</CardTitle>
              <PriorityDonut data={data.byPriority} total={total} />
            </Card>
            <Card className="p-4">
              <CardTitle className="text-sm">Por responsable</CardTitle>
              <HorizontalBars data={data.byTechnician} color="var(--chart-blue)" label="Solicitudes por responsable" />
            </Card>
            <Card className="p-4">
              <CardTitle className="mb-2 text-sm">Evolución en el tiempo</CardTitle>
              <TimelineChart series={data.timeline} startLabel={`hace ${data.rangeDays} d`} />
            </Card>
            <Card className="p-4">
              <CardTitle className="mb-2 text-sm">Cumplimiento de SLA</CardTitle>
              <SlaGauge pct={data.slaCompliancePct} />
            </Card>
            <Card className="p-4 [grid-column:1/-1]">
              <CardTitle className="mb-2 text-sm">Solicitudes por vencer en 24 h</CardTitle>
              {data.dueSoon.length === 0 && <div className="py-2 text-muted">Ninguna solicitud vence en las próximas 24 horas.</div>}
              {data.dueSoon.map((s) => {
                const r = withSla(toRequestRow(s), now)
                return (
                  <button
                    key={r.code}
                    type="button"
                    onClick={() => navigate(`/solicitudes/${r.code}`)}
                    className="flex w-full flex-wrap items-center gap-2.5 border-0 border-t border-line bg-transparent px-1 py-2 text-left text-fg"
                  >
                    <b className="w-[130px] text-[13px]">{r.code}</b>
                    <span className="min-w-[140px] flex-1">{r.category}</span>
                    <PriorityBadge priority={r.priority} />
                    <span className="w-[120px] text-xs text-muted">{r.technician ?? 'Sin asignar'}</span>
                    <span className="text-xs font-semibold" style={{ color: slaBarColor(r.sla.tone) }}>
                      {r.sla.label}
                    </span>
                  </button>
                )
              })}
            </Card>
          </div>
        </>
      )}
    </>
  )
}
