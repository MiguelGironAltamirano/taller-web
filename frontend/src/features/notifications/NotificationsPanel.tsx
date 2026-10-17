import { useState } from 'react'
import { useNavigate } from 'react-router'
import { cn } from '../../shared/lib/cn'
import { dateLong } from '../../shared/lib/format'
import type { NotificationItem } from '../../shared/domain'
import { Tabs } from '../../shared/ui'
import { useMarkAllRead, useMarkRead, useNotifications } from './api'

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
        className="fixed right-2 top-14 z-[41] w-[min(380px,calc(100%-16px))] animate-pop rounded-xl border border-line bg-surface shadow-pop md:right-4 md:top-16"
      >
        <div className="flex items-center gap-2 px-3.5 pb-2 pt-3">
          <b className="flex-1">Notificaciones</b>
          <button
            type="button"
            onClick={() => markAll.mutate()}
            className="border-0 bg-transparent text-xs text-primary-fg underline"
          >
            Marcar todas como leídas
          </button>
        </div>
        <div className="px-3.5 pb-2">
          <Tabs variant="pill" label="Filtro de notificaciones" items={FILTERS} value={filter} onChange={setFilter} />
        </div>
        <div className="max-h-[340px] overflow-auto border-t border-line">
          {items.length === 0 && <div className="p-6 text-center text-muted">Estás al día.</div>}
          {items.map((n) => (
            <button
              key={n.id}
              type="button"
              onClick={() => open(n)}
              className={cn(
                'flex w-full items-start gap-2.5 border-0 border-b border-line px-3.5 py-2.5 text-left text-fg',
                n.read ? 'bg-transparent' : 'bg-primary-soft',
              )}
            >
              <span className={cn('mt-1.5 size-2 shrink-0 rounded-full', n.read ? 'bg-transparent' : 'bg-primary')} />
              <span className={cn('flex-1 text-[13px] leading-snug', n.read ? 'font-normal' : 'font-semibold')}>
                {n.text}
                <span className="mt-0.5 block text-[11px] font-normal text-muted">{dateLong(n.at)}</span>
              </span>
            </button>
          ))}
        </div>
        <button
          type="button"
          onClick={() => {
            onClose()
            navigate('/notificaciones')
          }}
          className="h-10 w-full rounded-b-xl border-0 border-t border-line bg-transparent font-semibold text-primary-fg"
        >
          Ver todas
        </button>
      </div>
    </>
  )
}
