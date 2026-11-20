import { useEffect, useRef } from 'react'
import { useNavigate } from 'react-router'
import { QueryErrorState } from '../../shared/api/QueryErrorState'
import { cn } from '../../shared/lib/cn'
import { dateLong } from '../../shared/lib/format'
import type { NotificationItem } from '../../shared/domain'
import { Icon, SkeletonRows } from '../../shared/ui'
import { useMarkAllRead, useMarkRead, useNotifications } from './api'
import { notificationVisual, VISUAL_TONE } from './lib/visual'

/** Cuántas notificaciones muestra el panel rápido; el resto está en la página completa. */
export const PANEL_LIMIT = 5

/** Las no leídas primero y, dentro de cada grupo, las más recientes. */
function latest(items: NotificationItem[]): NotificationItem[] {
  return [...items].sort((a, b) => Number(a.read) - Number(b.read) || b.at - a.at).slice(0, PANEL_LIMIT)
}

type Props = {
  /** Cierra el panel porque la persona navega (a una solicitud o a «Ver todas»). */
  onClose: () => void
  /** Cierra el panel sin navegar (tocar fuera): quien lo abrió devuelve el foco a la campana. */
  onDismiss?: () => void
}

/**
 * Panel de la campana: las últimas notificaciones, «Marcar todo como leído» y «Ver todas».
 * Usa las mismas consultas que la página completa, así el contador de la campana se actualiza solo.
 */
export function NotificationsPanel({ onClose, onDismiss = onClose }: Props) {
  const navigate = useNavigate()
  const notifications = useNotifications()
  const { data = [], isLoading, isError } = notifications
  const markRead = useMarkRead()
  const markAll = useMarkAllRead()
  const panelRef = useRef<HTMLDivElement>(null)
  const items = latest(data)
  const unread = data.filter((n) => !n.read).length

  // El foco entra al panel al abrirlo, para que el teclado y el lector de pantalla lo encuentren.
  useEffect(() => {
    panelRef.current?.focus()
  }, [])

  const open = (n: NotificationItem) => {
    if (!n.read) markRead.mutate(n.id)
    onClose()
    if (n.requestCode) navigate(`/solicitudes/${n.requestCode}`)
  }

  return (
    <>
      <div className="fixed inset-0 z-40 bg-black/45 md:bg-transparent" onClick={onDismiss} aria-hidden="true" />
      <div
        ref={panelRef}
        tabIndex={-1}
        role="dialog"
        aria-label="Notificaciones"
        className="fixed left-2 right-2 top-14 z-[41] flex animate-pop flex-col overflow-hidden rounded-xl border border-line bg-surface shadow-pop outline-none md:left-auto md:right-4 md:top-16 md:w-[380px]"
      >
        <div className="flex items-center gap-2 px-4 pb-2.5 pt-3.5">
          <span className="flex flex-1 items-center gap-2 text-[15px] font-semibold">
            Notificaciones
            {unread > 0 && (
              <span className="rounded-full bg-primary px-1.5 py-0.5 text-[11px] font-bold tabular-nums text-on-primary">
                {unread}
              </span>
            )}
          </span>
          <button
            type="button"
            onClick={() => markAll.mutate()}
            disabled={unread === 0}
            className="min-h-11 rounded-md border-0 bg-transparent px-2 py-1 text-xs font-medium text-primary-fg hover:bg-primary-soft disabled:cursor-default disabled:text-muted disabled:hover:bg-transparent md:min-h-0"
          >
            Marcar todo como leído
          </button>
        </div>
        <div className="max-h-[min(60dvh,360px)] overflow-auto border-t border-line">
          {isLoading && <SkeletonRows count={3} rowClass="h-14" />}
          {isError && !notifications.data && (
            <div className="px-4 py-3">
              <QueryErrorState what="tus notificaciones" query={notifications} compact />
            </div>
          )}
          {!isLoading && !isError && items.length === 0 && (
            <div className="flex flex-col items-center gap-2 px-6 py-8 text-center text-muted">
              <span className="grid size-10 place-items-center rounded-full bg-ok-bg text-ok-fg">
                <Icon name="check" size={18} strokeWidth={2.25} />
              </span>
              No tienes notificaciones nuevas
            </div>
          )}
          {items.map((n) => {
            const visual = notificationVisual(n.text)
            return (
              <button
                key={n.id}
                type="button"
                onClick={() => open(n)}
                className={cn(
                  'flex min-h-11 w-full items-start gap-3 border-0 border-b border-line px-4 py-3 text-left text-fg last:border-0',
                  n.read ? 'bg-transparent hover:bg-surface2' : 'bg-primary-soft/40 hover:bg-primary-soft/70',
                )}
              >
                <span aria-hidden="true" className="relative mt-0.5 shrink-0">
                  <span
                    className={cn(
                      'grid size-8 place-items-center rounded-full',
                      n.read ? 'bg-surface2 text-muted' : VISUAL_TONE[visual.tone],
                    )}
                  >
                    <Icon name={visual.icon} size={15} strokeWidth={2} />
                  </span>
                  {!n.read && (
                    <span className="absolute -right-0.5 -top-0.5 size-2.5 rounded-full bg-primary ring-2 ring-surface" />
                  )}
                </span>
                <span className={cn('flex-1 text-[13px] leading-snug', n.read ? 'font-normal text-fg/85' : 'font-semibold')}>
                  {n.text}
                  <span className="mt-1 flex items-center gap-1 text-[11px] font-normal text-muted">
                    <Icon name="clock" size={11} />
                    {dateLong(n.at)}
                  </span>
                </span>
              </button>
            )
          })}
        </div>
        <button
          type="button"
          onClick={() => {
            onClose()
            navigate('/notificaciones')
          }}
          className="flex h-11 w-full items-center justify-center gap-1.5 border-0 border-t border-line bg-surface2/50 font-semibold text-primary-fg hover:bg-surface2"
        >
          Ver todas ({data.length})
          <Icon name="chevR" size={15} strokeWidth={2.25} />
        </button>
      </div>
    </>
  )
}
