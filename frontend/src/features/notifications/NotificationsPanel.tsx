import { useState } from 'react'
import { useNavigate } from 'react-router'
import { cn } from '../../shared/lib/cn'
import { dateLong } from '../../shared/lib/format'
import type { NotificationItem } from '../../shared/domain'
import { Icon, Tabs } from '../../shared/ui'
import { useMarkAllRead, useMarkRead, useNotifications } from './api'
import { notificationVisual, VISUAL_TONE } from './lib/visual'

type Filter = 'all' | 'unread'

const FILTERS = [
  { id: 'all' as const, label: 'Todas' },
  { id: 'unread' as const, label: 'No leídas' },
]

type Props = { onClose: () => void }

/** Panel desplegable de la campana: lista breve con "Marcar todas como leídas" y enlace a la página completa. */
export function NotificationsPanel({ onClose }: Props) {
  const navigate = useNavigate()
  const { data = [] } = useNotifications()
  const markRead = useMarkRead()
  const markAll = useMarkAllRead()
  const [filter, setFilter] = useState<Filter>('all')
  const items = data.filter((n) => filter === 'all' || !n.read)
  const unread = data.filter((n) => !n.read).length

  const open = (n: NotificationItem) => {
    if (!n.read) markRead.mutate(n.id)
    onClose()
    if (n.requestCode) navigate(`/solicitudes/${n.requestCode}`)
  }

  return (
    <>
      <div className="fixed inset-0 z-40" onClick={onClose} aria-hidden="true" />
      <div
        role="dialog"
        aria-label="Notificaciones"
        className="fixed right-2 top-14 z-[41] flex w-[min(400px,calc(100%-16px))] animate-pop flex-col overflow-hidden rounded-xl border border-line bg-surface shadow-pop md:right-4 md:top-16"
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
            className="rounded-md border-0 bg-transparent px-2 py-1 text-xs font-medium text-primary-fg hover:bg-primary-soft disabled:cursor-default disabled:text-muted disabled:hover:bg-transparent"
          >
            Marcar todas como leídas
          </button>
        </div>
        <div className="px-4 pb-2.5">
          <Tabs variant="pill" label="Filtro de notificaciones" items={FILTERS} value={filter} onChange={setFilter} />
        </div>
        <div className="max-h-[360px] overflow-auto border-t border-line">
          {items.length === 0 && (
            <div className="flex flex-col items-center gap-2 px-6 py-8 text-center text-muted">
              <span className="grid size-10 place-items-center rounded-full bg-ok-bg text-ok-fg">
                <Icon name="check" size={18} strokeWidth={2.25} />
              </span>
              Estás al día.
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
                  'flex w-full items-start gap-3 border-0 border-b border-line px-4 py-3 text-left text-fg last:border-0',
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
          Ver todas
          <Icon name="chevR" size={15} strokeWidth={2.25} />
        </button>
      </div>
    </>
  )
}
