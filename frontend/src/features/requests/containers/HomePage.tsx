import { useMemo } from 'react'
import { Link, useNavigate } from 'react-router'
import { QueryErrorState } from '../../../shared/api/QueryErrorState'
import { useNow } from '../../../shared/hooks/useNow'
import { dateFull, dateLong } from '../../../shared/lib/format'
import { Card, CardTitle, Icon, KpiCard, PageHeader } from '../../../shared/ui'
import { useSessionUser } from '../../auth'
import { useNotifications } from '../../notifications'
import { useRequestDraft, useRequestRows } from '../api'
import { AttentionPanel } from '../components/AttentionPanel'
import { attentionFor, sortByUrgency, useUnseenCodes } from '../lib/attention'
import { isOpen, withSla } from '../lib/view'

/** Inicio del solicitante: resumen de sus solicitudes, lo que requiere atención y actividad reciente. */
export function HomePage() {
  const user = useSessionUser()
  const navigate = useNavigate()
  const now = useNow()
  const rowsQuery = useRequestRows()
  const { data: rows } = rowsQuery
  const { data: draft } = useRequestDraft()
  const { data: notifications = [] } = useNotifications()

  const views = useMemo(() => (rows ?? []).map((r) => withSla(r, now)), [rows, now])
  const kpis = [
    { label: 'Abiertas', value: views.filter(isOpen).length, to: '/solicitudes?view=open', icon: 'folder', tone: 'bg-info-bg text-info-fg' },
    { label: 'Por confirmar', value: views.filter((r) => r.state === 'Resuelta').length, to: '/solicitudes?view=confirm', icon: 'checkCircle', tone: 'bg-ok-bg text-ok-fg' },
    { label: 'Cerradas', value: views.filter((r) => r.state === 'Cerrada').length, to: '/solicitudes?status=Cerrada', icon: 'lock', tone: 'bg-surface2 text-muted' },
  ] as const
  const unseen = useUnseenCodes()
  const attention = sortByUrgency(
    views.filter((r) => attentionFor(user.role, r, unseen)),
    unseen,
  ).map((row) => ({ row, attention: attentionFor(user.role, row, unseen)!, unseen: unseen.has(row.code) }))

  return (
    <>
      <PageHeader
        icon="home"
        title={`Hola, ${user.name.split(' ')[0]}`}
        subtitle={`Resumen de tus solicitudes al ${dateFull(now)}.`}
      />
      {draft && (
        <section
          aria-label="Borrador sin enviar"
          className="flex flex-wrap items-center gap-3 rounded-xl border border-line bg-info-bg px-4 py-3 text-info-fg"
        >
          <Icon name="file" size={18} className="shrink-0" />
          <p className="m-0 min-w-0 flex-1 text-[13px]">
            <b>Tienes una solicitud sin enviar</b>, guardada el {dateLong(Date.parse(draft.updatedAt))}
            {draft.description.trim() && <>: «{draft.description.trim().slice(0, 80)}{draft.description.trim().length > 80 ? '…' : ''}»</>}
          </p>
          <Link
            to="/solicitudes/nueva?retomar=1"
            className="inline-flex h-8 items-center rounded-lg bg-primary px-3 text-[13px] font-semibold text-on-primary no-underline hover:opacity-90"
          >
            Retomar borrador
          </Link>
        </section>
      )}
      {rowsQuery.isError && !rows && (
        <Card>
          <QueryErrorState what="el resumen de tus solicitudes" query={rowsQuery} compact />
        </Card>
      )}
      <div className="grid grid-cols-[repeat(auto-fit,minmax(140px,1fr))] gap-3 md:grid-cols-[repeat(auto-fit,minmax(200px,1fr))]">
        {kpis.map((k) => (
          <KpiCard
            key={k.label}
            label={k.label}
            value={k.value}
            icon={k.icon}
            iconClassName={k.tone}
            onClick={() => navigate(k.to)}
          />
        ))}
      </div>
      <div className="flex flex-wrap items-start gap-3.5">
        <div className="min-w-0 flex-[2_1_560px]">
          {attention.length > 0 ? (
            <AttentionPanel
              items={attention}
              max={6}
              onOpen={(code) => navigate(`/solicitudes/${code}`)}
              onShowAll={() => navigate('/solicitudes?view=attention')}
            />
          ) : (
            <Card className="flex items-center gap-3 p-4">
              <span className="grid size-10 shrink-0 place-items-center rounded-full bg-ok-bg text-ok-fg">
                <Icon name="check" size={18} strokeWidth={2.25} />
              </span>
              <div>
                <div className="font-semibold">Todo al día</div>
                <div className="text-[13px] text-muted">No tienes solicitudes que requieran tu acción.</div>
              </div>
            </Card>
          )}
        </div>
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
