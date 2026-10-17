import { useCatalogs } from '../../shared/api/catalogs'
import { cn } from '../../shared/lib/cn'
import { Avatar, Card, EmptyState, GridRow, GridTable, PageHeader, ProgressBar, SkeletonRows } from '../../shared/ui'
import { useSessionUser } from '../auth'
import { useTeam } from './api'

const COLUMNS = '2fr 1fr 1fr 1fr 1.6fr'

/** Carga actual por técnico del área del supervisor. */
export function TeamPage() {
  const user = useSessionUser()
  const { data: catalogs } = useCatalogs()
  const { data: team = [], isLoading } = useTeam()
  const area = catalogs?.areas.find((a) => a.id === user.areaId)?.name ?? ''

  return (
    <>
      <PageHeader title="Equipo" subtitle={`Carga actual por técnico · ${area}`} />
      <Card className="overflow-hidden">
        {isLoading && <SkeletonRows count={4} />}
        {!isLoading && team.length === 0 && <EmptyState icon="users" title="Sin técnicos en tu área" compact />}
        {team.length > 0 && (
          <GridTable columns={COLUMNS} minWidth="680px" label="Carga por técnico" heads={['Técnico', 'Asignadas', 'En atención', 'Vencidas', 'Carga']}>
            {team.map((t) => {
              const pct = Math.min(100, (t.assigned / t.capacity) * 100)
              const color = t.assigned >= 5 ? 'var(--bad)' : t.assigned >= 3 ? 'var(--warn)' : 'var(--good)'
              return (
                <GridRow key={t.id} columns={COLUMNS} className="min-h-12">
                  <div className="flex min-w-0 items-center gap-2">
                    <Avatar name={t.fullName} size="sm" />
                    <span className="truncate">{t.fullName}</span>
                  </div>
                  <div>{t.assigned}</div>
                  <div>{t.inProgress}</div>
                  <div className={cn(t.overdue > 0 && 'font-semibold text-err-fg')}>{t.overdue}</div>
                  <div className="flex items-center gap-2">
                    <ProgressBar value={pct} color={color} label={`Carga de ${t.fullName}`} className="flex-1" />
                    <span className="whitespace-nowrap text-xs text-muted">
                      {t.assigned} de {t.capacity}
                    </span>
                  </div>
                </GridRow>
              )
            })}
          </GridTable>
        )}
      </Card>
    </>
  )
}
