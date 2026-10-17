import { useMemo } from 'react'
import { useCatalogs } from '../../shared/api/catalogs'
import { useNow } from '../../shared/hooks/useNow'
import { downloadText } from '../../shared/lib/download'
import { Button, Card, GridRow, GridTable, PageHeader, useToast } from '../../shared/ui'
import { useSessionUser } from '../auth'
import { useRequestRows, withSla } from '../requests'
import { useTeam } from './api'
import { REPORTS, buildReport, type ReportId } from './lib/reports'

const COLUMNS = '1.4fr 3fr 170px'

/** Reportes descargables del área del supervisor (CSV; el PDF queda pendiente). */
export function ReportsPage() {
  const user = useSessionUser()
  const toast = useToast()
  const now = useNow()
  const { data: catalogs } = useCatalogs()
  const { data: rows } = useRequestRows()
  const { data: team = [] } = useTeam()
  const views = useMemo(() => (rows ?? []).map((r) => withSla(r, now)), [rows, now])
  const area = catalogs?.areas.find((a) => a.id === user.areaId)?.name ?? ''

  const download = (id: ReportId, title: string) => {
    const { filename, csv } = buildReport(id, views, team)
    downloadText(filename, csv)
    toast(`${title} exportado (CSV)`)
  }

  return (
    <>
      <PageHeader title="Reportes" subtitle={`Descarga la información de ${area}`} />
      <Card className="overflow-hidden">
        <GridTable columns={COLUMNS} minWidth="640px" label="Reportes" heads={['Reporte', 'Contenido', '']}>
          {REPORTS.map((r) => (
            <GridRow key={r.id} columns={COLUMNS} className="min-h-12 py-1.5">
              <div className="font-semibold">{r.title}</div>
              <div>{r.description}</div>
              <div className="flex gap-2">
                <Button size="sm" onClick={() => download(r.id, r.title)}>
                  CSV
                </Button>
                <Button size="sm" onClick={() => toast('La exportación a PDF aún no está disponible.', { bad: true })}>
                  PDF
                </Button>
              </div>
            </GridRow>
          ))}
        </GridTable>
      </Card>
    </>
  )
}
