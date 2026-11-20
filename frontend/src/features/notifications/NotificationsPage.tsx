import { useState } from 'react'
import { useLocation, useNavigate } from 'react-router'
import { homePathFor, useSessionUser } from '../auth'
import { QueryErrorState } from '../../shared/api/QueryErrorState'
import { cn } from '../../shared/lib/cn'
import { dateLong, dayGroup } from '../../shared/lib/format'
import { useNow } from '../../shared/hooks/useNow'
import { Button, Card, EmptyState, Icon, PageHeader, Tabs } from '../../shared/ui'
import { useMarkAllRead, useMarkRead, useNotifications, useUnreadCount } from './api'
import { notificationVisual, VISUAL_TONE } from './lib/visual'

const FILTERS = [
  { id: 'all' as const, label: 'Todas' },
  { id: 'unread' as const, label: 'No leídas' },
]
const GROUPS = ['Hoy', 'Ayer', 'Anteriores'] as const

export function NotificationsPage() {
  const navigate = useNavigate()
  const location = useLocation()
  const user = useSessionUser()
  const now = useNow()
  const notifications = useNotifications()
  const { data = [], isLoading, isError } = notifications
  const unread = useUnreadCount()
  const markRead = useMarkRead()
  const markAll = useMarkAllRead()
  const [filter, setFilter] = useState<'all' | 'unread'>('all')

  const visible = data.filter((n) => filter === 'all' || !n.read)
  const groups = GROUPS.map((label) => ({ label, items: visible.filter((n) => dayGroup(n.at, now) === label) })).filter(
    (g) => g.items.length,
  )

  // Sin pestaña propia en móvil: un botón claro para volver (a la pantalla anterior, o al inicio si se entró directo).
  const goBack = () => (location.key === 'default' ? navigate(homePathFor(user.role)) : navigate(-1))

  return (
    <>
      <Button variant="ghost" className="self-start md:hidden" onClick={goBack}>
        <Icon name="arrowL" size={16} strokeWidth={2} />
        Volver
      </Button>
      <PageHeader
        icon="bell"
        title="Notificaciones"
        subtitle={`${unread} sin leer`}
        actions={
          <Button onClick={() => markAll.mutate()} disabled={unread === 0} loading={markAll.isPending}>
            <Icon name="check" size={16} strokeWidth={2} />
            Marcar todas como leídas
          </Button>
        }
      />
      <Tabs variant="pill" label="Filtro de notificaciones" items={FILTERS} value={filter} onChange={setFilter} />
      {isError && !notifications.data && (
        <Card>
          <QueryErrorState what="tus notificaciones" query={notifications} />
        </Card>
      )}
      {!isLoading && !(isError && !notifications.data) && groups.length === 0 && (
        <Card>
          <EmptyState
            icon="bell"
            title={filter === 'unread' ? 'No hay notificaciones sin leer.' : 'No tienes notificaciones.'}
            description="Aquí verás los cambios de tus solicitudes, las asignaciones y los comentarios."
          />
        </Card>
      )}
      {groups.map((g) => (
        <section key={g.label} aria-labelledby={`notif-${g.label}`} className="flex flex-col gap-2">
          <h2
            id={`notif-${g.label}`}
            className="m-0 mt-1 flex items-center gap-2 text-[11px] font-semibold uppercase tracking-wider text-muted"
          >
            {g.label}
            <span className="rounded-md bg-surface2 px-1.5 py-0.5 tabular-nums">{g.items.length}</span>
            <span aria-hidden="true" className="h-px flex-1 bg-line" />
          </h2>
          <Card className="overflow-hidden">
            <ul className="m-0 list-none p-0">
              {g.items.map((n) => {
                const visual = notificationVisual(n.text)
                return (
                  <li
                    key={n.id}
                    className={cn(
                      'group flex items-center gap-3.5 border-b border-line px-4 py-3.5 transition-colors last:border-0',
                      n.read ? 'hover:bg-surface2/60' : 'bg-primary-soft/40 hover:bg-primary-soft/70',
                    )}
                  >
                    <span aria-hidden="true" className="relative shrink-0">
                      <span
                        className={cn(
                          'grid size-10 place-items-center rounded-full',
                          n.read ? 'bg-surface2 text-muted' : VISUAL_TONE[visual.tone],
                        )}
                      >
                        <Icon name={visual.icon} size={18} strokeWidth={1.9} />
                      </span>
                      {!n.read && (
                        <span className="absolute -right-0.5 -top-0.5 size-3 rounded-full bg-primary ring-2 ring-surface" />
                      )}
                    </span>
                    <button
                      type="button"
                      onClick={() => {
                        if (!n.read) markRead.mutate(n.id)
                        if (n.requestCode) navigate(`/solicitudes/${n.requestCode}`)
                      }}
                      className="min-w-0 flex-1 border-0 bg-transparent p-0 text-left text-fg"
                    >
                      <span className={cn('block leading-snug', n.read ? 'font-normal text-fg/85' : 'font-semibold')}>
                        {n.text}
                      </span>
                      <span className="mt-1 flex items-center gap-1.5 text-xs text-muted">
                        <Icon name="clock" size={12} />
                        {dateLong(n.at)}
                      </span>
                    </button>
                    {!n.read && (
                      <Button size="sm" variant="ghost" onClick={() => markRead.mutate(n.id)}>
                        <Icon name="check" size={14} strokeWidth={2.25} />
                        Marcar como leída
                      </Button>
                    )}
                  </li>
                )
              })}
            </ul>
          </Card>
        </section>
      ))}
    </>
  )
}
