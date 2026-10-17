import { useState } from 'react'
import { dateLong } from '../../shared/lib/format'
import { Button, Card, EmptyState, GridRow, GridTable, PageHeader, SkeletonRows, useToast } from '../../shared/ui'
import { buildCsv } from '../../shared/lib/csv'
import { downloadText } from '../../shared/lib/download'
import { useAudit } from './api'

const COLUMNS = '130px 1.3fr 130px 3fr'

/** Registro de cambios sobre solicitudes: quién, cuándo y qué cambió (trazabilidad del enunciado). */
export function AuditPage() {
  const toast = useToast()
  const [q, setQ] = useState('')
  const { data: events = [], isLoading } = useAudit(q.trim())

  const exportCsv = () => {
    downloadText(
      `auditoria-${new Date().toISOString().slice(0, 10)}.csv`,
      buildCsv(['Fecha', 'Usuario', 'Solicitud', 'Cambio'], events.map((e) => [dateLong(Date.parse(e.createdAt)), e.actorName, e.requestCode, e.detail])),
    )
    toast('Auditoría exportada (CSV)')
  }

  return (
    <>
      <PageHeader
        title="Auditoría"
        subtitle="Registro de cambios sobre solicitudes"
        actions={
          <Button onClick={exportCsv} disabled={events.length === 0}>
            Exportar
          </Button>
        }
      />
      <input
        type="search"
        aria-label="Buscar"
        value={q}
        onChange={(e) => setQ(e.target.value)}
        placeholder="Buscar por usuario, código o acción"
        className="h-9 w-full rounded-lg border border-line bg-surface px-2.5 max-md:h-11"
      />
      <Card className="overflow-hidden">
        {isLoading && <SkeletonRows count={6} />}
        {!isLoading && events.length === 0 && <EmptyState icon="shield" title="Sin resultados" compact />}
        {events.length > 0 && (
          <GridTable columns={COLUMNS} minWidth="860px" label="Auditoría" heads={['Fecha', 'Usuario', 'Solicitud', 'Cambio']}>
            {events.map((e) => (
              <GridRow key={e.id} columns={COLUMNS} className="min-h-12 py-1.5">
                <div>{dateLong(Date.parse(e.createdAt))}</div>
                <div className="truncate">{e.actorName}</div>
                <div>{e.requestCode}</div>
                <div>{e.detail}</div>
              </GridRow>
            ))}
          </GridTable>
        )}
      </Card>
    </>
  )
}
