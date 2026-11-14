import { useState } from 'react'
import { useNavigate, useParams } from 'react-router'
import { ApiError } from '../../../shared/api/client'
import { REQUEST_STATES, stateIndex, type RequestDetailView } from '../../../shared/domain'
import { useNow } from '../../../shared/hooks/useNow'
import { dateLong } from '../../../shared/lib/format'
import {
  Avatar,
  Button,
  Card,
  EmptyState,
  Icon,
  IconButton,
  type IconName,
  OverdueBadge,
  PriorityBadge,
  ProgressBar,
  Skeleton,
  Stepper,
  StatusBadge,
  stateIcon,
  Tabs,
  Timeline,
  useToast,
} from '../../../shared/ui'
import { ErrorPage, useSessionUser } from '../../auth'
import { CommentsTab, EvidenceTab } from '../../collaboration'
import { SatisfactionPanel } from '../../satisfaction'
import { ActionsPanel } from '../../workflow'
import { useRequestDetail } from '../api'
import type { SlaStatus, SlaTone } from '../../workflow/lib/sla'
import { slaBarColor, withSla } from '../lib/view'
import { cn } from '../../../shared/lib/cn'

type Tab = 'sum' | 'com' | 'ev' | 'hist'

const STEP_ICONS = REQUEST_STATES.map(stateIcon)

/** Fecha en que la solicitud alcanzó cada etapa ya recorrida (la más reciente si se repitió). */
function stepCaptions(r: RequestDetailView): (string | undefined)[] {
  const current = stateIndex(r.state)
  return REQUEST_STATES.map((state, i) => {
    if (i > current) return undefined
    const entry = [...r.history].reverse().find((h) => h.to === state)
    const at = entry?.at ?? (i === 0 ? r.createdAt : undefined)
    return at ? dateLong(at) : undefined
  })
}

const SECTION_LABEL = 'm-0 text-[11px] font-semibold uppercase tracking-wider text-muted'

function SummaryTab({ r }: { r: RequestDetailView }) {
  return (
    <div className="flex flex-col gap-5">
      <section aria-labelledby="desc-title" className="flex flex-col gap-2">
        <h3 id="desc-title" className={SECTION_LABEL}>
          Descripción
        </h3>
        <p className="m-0 whitespace-pre-line rounded-lg border-l-[3px] border-primary/60 bg-surface2/70 px-4 py-3 text-[14px] leading-relaxed">
          {r.description}
        </p>
      </section>

      <dl className="m-0 grid overflow-hidden rounded-lg border border-line sm:grid-cols-3">
        <div className="flex flex-col gap-1.5 p-3.5 max-sm:border-b max-sm:border-line sm:border-r sm:border-line">
          <dt className="flex items-center gap-1.5 text-xs text-muted">
            <Icon name="tag" size={14} />
            Categoría
          </dt>
          <dd className="m-0 font-medium">{r.category}</dd>
        </div>
        <div className="flex flex-col gap-1.5 p-3.5 max-sm:border-b max-sm:border-line sm:border-r sm:border-line">
          <dt className="flex items-center gap-1.5 text-xs text-muted">
            <Icon name="mapPin" size={14} />
            Ubicación
          </dt>
          <dd className="m-0">
            <span className="font-medium">{r.room}</span>
            <span className="text-muted"> · {r.pavilion}</span>
            <span className="mt-0.5 block text-xs text-muted">{r.faculty}</span>
          </dd>
        </div>
        <div className="flex flex-col gap-1.5 p-3.5">
          <dt className="flex items-center gap-1.5 text-xs text-muted">
            <Icon name="userCheck" size={14} />
            Técnico responsable
          </dt>
          <dd className="m-0">
            {r.technician ? (
              <span className="flex items-center gap-2 font-medium">
                <Avatar name={r.technician} size="xs" tone="soft" />
                {r.technician}
              </span>
            ) : (
              <span className="text-muted">Sin asignar</span>
            )}
          </dd>
        </div>
      </dl>
    </div>
  )
}

const SLA_TONE: Record<SlaTone, { panel: string; text: string; icon: IconName }> = {
  ok: { panel: 'border-line bg-surface2/50', text: 'text-ok-fg', icon: 'clock' },
  warn: { panel: 'border-warn/40 bg-warn-bg/60', text: 'text-warn-fg', icon: 'alert' },
  bad: { panel: 'border-err-fg/30 bg-err-bg/60', text: 'text-err-fg', icon: 'alert' },
  done: { panel: 'border-line bg-ok-bg/50', text: 'text-ok-fg', icon: 'checkCircle' },
}

function SlaPanel({ r, sla }: { r: RequestDetailView; sla: SlaStatus }) {
  const tone = SLA_TONE[sla.tone]
  const hours = Math.round((r.dueAt - r.createdAt) / 3_600_000)
  return (
    <section aria-labelledby="sla-title" className={cn('flex flex-col gap-2.5 rounded-lg border p-3.5', tone.panel)}>
      <div className="flex items-center justify-between gap-2">
        <h2 id="sla-title" className={SECTION_LABEL}>
          Plazo de atención · {hours} h
        </h2>
        <span className="text-xs tabular-nums text-muted">{Math.round(sla.pct)}%</span>
      </div>
      <div className={cn('flex items-center gap-2 text-[15px] font-semibold', tone.text)}>
        <Icon name={tone.icon} size={18} strokeWidth={2} />
        {sla.label}
      </div>
      <ProgressBar
        value={sla.pct}
        color={slaBarColor(sla.tone)}
        label="Plazo de atención consumido"
        className="h-2"
      />
      <div className="flex justify-between gap-2 text-[11px] tabular-nums text-muted">
        <span>Inicio {dateLong(r.createdAt)}</span>
        <span>Vence {dateLong(r.dueAt)}</span>
      </div>
    </section>
  )
}

function DetailRow({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div className="flex min-h-10 items-center justify-between gap-3 border-b border-line py-2 last:border-0">
      <dt className="shrink-0 text-muted">{label}</dt>
      <dd className="m-0 min-w-0 text-right font-medium">{children}</dd>
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

  return (
    <>
      <div className="flex flex-wrap items-center gap-2.5">
        <Button variant="secondary" className="pl-2.5" onClick={() => navigate('/solicitudes')}>
          <Icon name="arrowL" size={16} strokeWidth={2} />
          Volver
        </Button>
        <span aria-hidden="true" className="h-6 w-px bg-line" />
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
          <SatisfactionPanel request={request} user={user} />
          <Card className="overflow-x-auto px-4 pb-4 pt-5">
            <Stepper
              steps={REQUEST_STATES}
              current={stateIndex(request.state)}
              icons={STEP_ICONS}
              captions={stepCaptions(request)}
              label="Progreso de la solicitud"
            />
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

        <Card className="sticky top-0 flex min-w-0 flex-[1_1_280px] flex-col gap-4 p-4" aria-label="Detalle">
          <SlaPanel r={request} sla={view.sla} />
          <dl className="m-0 flex flex-col text-[13px]">
            <DetailRow label="Estado">
              <StatusBadge state={request.state} />
            </DetailRow>
            <DetailRow label="Prioridad">
              <PriorityBadge priority={request.priority} />
            </DetailRow>
            <DetailRow label="Responsable">
              {request.technician ? (
                <span className="inline-flex items-center gap-2">
                  <Avatar name={request.technician} size="xs" tone="soft" />
                  <span className="truncate">{request.technician}</span>
                </span>
              ) : (
                <span className="font-normal text-muted">Sin asignar</span>
              )}
            </DetailRow>
            <DetailRow label="Área">{request.area}</DetailRow>
            <DetailRow label="Ubicación">
              {request.room}, {request.pavilion}
            </DetailRow>
            <DetailRow label="Registrada">
              <span className="tabular-nums">{dateLong(request.createdAt)}</span>
            </DetailRow>
            <DetailRow label="Solicitante">{request.requester}</DetailRow>
          </dl>
          <ActionsPanel request={request} />
        </Card>
      </div>
    </>
  )
}
