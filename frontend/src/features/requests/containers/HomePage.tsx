import { useMemo } from 'react'
import { useNavigate } from 'react-router'
import { stateIndex } from '../../../shared/domain'
import { useNow } from '../../../shared/hooks/useNow'
import { dateFull, dateLong } from '../../../shared/lib/format'
import { Card, CardTitle, KpiCard, OverdueBadge, StatusBadge } from '../../../shared/ui'
import { useSessionUser } from '../../auth'
import { useNotifications } from '../../notifications'
import { useRequestRows } from '../api'
import { isOpen, withSla } from '../lib/view'

/** Inicio del solicitante: resumen de sus solicitudes, lo que requiere atención y actividad reciente. */
export function HomePage() {
  const user = useSessionUser()
  const navigate = useNavigate()
  const now = useNow()
  const { data: rows } = useRequestRows()
  const { data: notifications = [] } = useNotifications()

  const views = useMemo(() => (rows ?? []).map((r) => withSla(r, now)), [rows, now])
  const kpis = [
    { label: 'Abiertas', value: views.filter(isOpen).length, to: '/solicitudes?view=open', tone: '' },
    { label: 'Por confirmar', value: views.filter((r) => r.state === 'Resuelta').length, to: '/solicitudes?view=confirm', tone: 'text-good' },
    { label: 'Vencidas', value: views.filter((r) => r.sla.overdue).length, to: '/solicitudes?view=venc', tone: 'text-err-fg' },
    { label: 'Cerradas', value: views.filter((r) => r.state === 'Cerrada').length, to: '/solicitudes?status=Cerrada', tone: 'text-muted' },
  ]
  const attention = views
    .filter((r) => r.state === 'Resuelta' || r.sla.overdue)
    .sort((a, b) => stateIndex(b.state) - stateIndex(a.state))
    .slice(0, 6)

  return (
    <>
      <div>
        <h1 className="m-0 text-xl font-semibold md:text-2xl">Hola, {user.name.split(' ')[0]}</h1>
        <div className="mt-0.5 text-muted">Resumen de tus solicitudes al {dateFull(now)}.</div>
      </div>
      <div className="grid grid-cols-[repeat(auto-fit,minmax(140px,1fr))] gap-3 md:grid-cols-[repeat(auto-fit,minmax(200px,1fr))]">
        {kpis.map((k) => (
          <KpiCard key={k.label} label={k.label} value={k.value} valueClassName={k.tone} onClick={() => navigate(k.to)} />
        ))}
      </div>
      <div className="flex flex-wrap items-start gap-3.5">
        <Card className="min-w-0 flex-[2_1_560px] p-4">
          <CardTitle className="mb-2.5 text-base">Requieren tu atención</CardTitle>
          {attention.length === 0 && <div className="py-4 text-muted">No tienes acciones pendientes.</div>}
          <div className="flex flex-col">
            {attention.map((r) => (
              <button
                key={r.code}
                type="button"
                onClick={() => navigate(`/solicitudes/${r.code}`)}
                className="flex flex-wrap items-center gap-2.5 border-0 border-t border-line bg-transparent px-1 py-2.5 text-left text-fg hover:bg-surface2"
              >
                <span className="w-[130px] text-[13px] font-semibold">{r.code}</span>
                <span className="min-w-[140px] flex-1">
                  {r.category}
                  <span className="text-muted"> · {r.room}</span>
                </span>
                <StatusBadge state={r.state} />
                {r.sla.overdue && <OverdueBadge />}
              </button>
            ))}
          </div>
        </Card>
        <Card className="min-w-0 flex-[1_1_280px] p-4">
          <CardTitle className="mb-2.5 text-base">Actividad reciente</CardTitle>
          <div className="flex flex-col gap-3">
            {notifications.length === 0 && <div className="text-muted">Sin actividad reciente.</div>}
            {notifications.slice(0, 5).map((n) => (
              <div key={n.id} className="flex gap-2.5">
                <span className="mt-1.5 size-2 shrink-0 rounded-full bg-primary" />
                <div className="text-[13px] leading-snug">
                  {n.text}
                  <div className="text-xs text-muted">{dateLong(n.at)}</div>
                </div>
              </div>
            ))}
          </div>
        </Card>
      </div>
    </>
  )
}
