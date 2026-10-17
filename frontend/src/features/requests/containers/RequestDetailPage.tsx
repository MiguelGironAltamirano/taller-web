import { useState } from 'react'
import { useNavigate, useParams } from 'react-router'
import { ApiError } from '../../../shared/api/client'
import { REQUEST_STATES, stateIndex, type RequestDetailView } from '../../../shared/domain'
import { useNow } from '../../../shared/hooks/useNow'
import { dateLong } from '../../../shared/lib/format'
import {
  Card,
  EmptyState,
  Icon,
  IconButton,
  OverdueBadge,
  PriorityBadge,
  ProgressBar,
  Skeleton,
  Stepper,
  StatusBadge,
  Tabs,
  Timeline,
  useToast,
} from '../../../shared/ui'
import { ErrorPage, useSessionUser } from '../../auth'
import { CommentsTab, EvidenceTab } from '../../collaboration'
import { ResolvedCard } from '../../satisfaction'
import { ActionsPanel } from '../../workflow'
import { useRequestDetail } from '../api'
import { slaBarColor, slaTextClass, withSla } from '../lib/view'
import { cn } from '../../../shared/lib/cn'

type Tab = 'sum' | 'com' | 'ev' | 'hist'

function SummaryTab({ r }: { r: RequestDetailView }) {
  return (
    <div className="flex flex-col gap-3.5">
      <div>
        <div className="mb-1 text-xs text-muted">Descripción</div>
        <div className="leading-relaxed">{r.description}</div>
      </div>
      <dl className="m-0 grid grid-cols-[repeat(auto-fit,minmax(200px,1fr))] gap-3">
        <div>
          <dt className="text-xs text-muted">Categoría</dt>
          <dd className="m-0 mt-0.5">{r.category}</dd>
        </div>
        <div>
          <dt className="text-xs text-muted">Ubicación</dt>
          <dd className="m-0 mt-0.5">{`${r.faculty} → ${r.pavilion} → ${r.room}`}</dd>
        </div>
        <div>
          <dt className="text-xs text-muted">Técnico responsable</dt>
          <dd className="m-0 mt-0.5">{r.technician ?? 'Sin asignar'}</dd>
        </div>
      </dl>
    </div>
  )
}

function HistoryTab({ r }: { r: RequestDetailView }) {
  const items = [...r.history].reverse().map((h, i) => ({
    id: i,
    title: (
      <>
        {h.from}
        <span className="font-normal text-muted"> → </span>
        {h.to}
      </>
    ),
    body: h.reason,
    meta: `${h.who} · ${dateLong(h.at)}`,
  }))
  return <Timeline items={items} />
}

/** Detalle de la solicitud: estado, historial, comentarios, evidencias y acciones según el rol. */
export function RequestDetailPage() {
  const { code } = useParams()
  const navigate = useNavigate()
  const toast = useToast()
  const user = useSessionUser()
  const now = useNow()
  const [tab, setTab] = useState<Tab>('sum')
  const { data: request, isLoading, error } = useRequestDetail(code)

  if (isLoading) {
    return (
      <div className="flex flex-col gap-3.5" role="status" aria-label="Cargando solicitud">
        <Skeleton className="h-9 w-64" />
        <Skeleton className="h-24" />
        <Skeleton className="h-64" />
      </div>
    )
  }
  if (error instanceof ApiError && (error.status === 404 || error.status === 403)) return <ErrorPage kind="404" />
  if (!request) {
    return <EmptyState title="No pudimos cargar la solicitud" description="Inténtalo de nuevo en unos momentos." />
  }

  const view = withSla(request, now)
  const copyCode = async () => {
    try {
      await navigator.clipboard.writeText(request.code)
    } catch {
      /* portapapeles no disponible */
    }
    toast('Código copiado')
  }
  const showResolved = user.role === 'solicitante' && request.requesterId === user.id && request.state === 'Resuelta'

  return (
    <>
      <div className="flex flex-wrap items-center gap-2.5">
        <IconButton label="Volver" variant="secondary" onClick={() => navigate('/solicitudes')}>
          <Icon name="chevR" size={16} style={{ transform: 'rotate(180deg)' }} />
        </IconButton>
        <h1 className="m-0 text-xl font-semibold md:text-2xl">{request.code}</h1>
        <IconButton label="Copiar código" onClick={copyCode}>
          <Icon name="copy" size={16} />
        </IconButton>
        <StatusBadge state={request.state} />
        <PriorityBadge priority={request.priority} />
        {view.sla.overdue && <OverdueBadge />}
      </div>

      <div className="flex flex-wrap items-start gap-3.5">
        <div className="flex min-w-0 flex-[2_1_560px] flex-col gap-3.5">
          {showResolved && <ResolvedCard request={request} />}
          <Card className="overflow-x-auto px-4 pb-3 pt-4">
            <Stepper steps={REQUEST_STATES} current={stateIndex(request.state)} />
          </Card>
          <Card>
            <Tabs
              label="Secciones de la solicitud"
              value={tab}
              onChange={setTab}
              items={[
                { id: 'sum', label: 'Resumen' },
                { id: 'com', label: 'Comentarios', count: request.comments.length },
                { id: 'ev', label: 'Evidencias', count: request.evidences.length },
                { id: 'hist', label: 'Historial', count: request.history.length },
              ]}
            />
            <div className="p-4" role="tabpanel">
              {tab === 'sum' && <SummaryTab r={request} />}
              {tab === 'com' && <CommentsTab request={request} currentUserName={user.name} />}
              {tab === 'ev' && <EvidenceTab request={request} />}
              {tab === 'hist' && <HistoryTab r={request} />}
            </div>
          </Card>
        </div>

        <Card className="sticky top-0 flex min-w-0 flex-[1_1_280px] flex-col gap-3 p-4" aria-label="Detalle">
          <div>
            <div className="mb-1.5 flex justify-between text-[13px]">
              <b>SLA</b>
              <span className={cn('font-semibold', slaTextClass(view.sla.tone))}>{view.sla.label}</span>
            </div>
            <ProgressBar value={view.sla.pct} color={slaBarColor(view.sla.tone)} label="Plazo de atención consumido" className="h-2" />
            <div className="mt-1.5 text-xs text-muted">Vence: {dateLong(request.dueAt)}</div>
          </div>
          <dl className="m-0 grid grid-cols-[auto_1fr] gap-x-3 gap-y-2 border-t border-line pt-3 text-[13px]">
            <dt className="text-muted">Estado</dt>
            <dd className="m-0">{request.state}</dd>
            <dt className="text-muted">Prioridad</dt>
            <dd className="m-0">
              {request.priority} · {Math.round((request.dueAt - request.createdAt) / 3_600_000)} h
            </dd>
            <dt className="text-muted">Responsable</dt>
            <dd className="m-0">{request.technician ?? 'Sin asignar'}</dd>
            <dt className="text-muted">Área</dt>
            <dd className="m-0">{request.area}</dd>
            <dt className="text-muted">Ubicación</dt>
            <dd className="m-0">
              {request.room}, {request.pavilion}
            </dd>
            <dt className="text-muted">Registrada</dt>
            <dd className="m-0">{dateLong(request.createdAt)}</dd>
            <dt className="text-muted">Solicitante</dt>
            <dd className="m-0">{request.requester}</dd>
          </dl>
          <ActionsPanel request={request} />
        </Card>
      </div>
    </>
  )
}
