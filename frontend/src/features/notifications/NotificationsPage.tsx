import { useState } from 'react'
import { useNavigate } from 'react-router'
import { cn } from '../../shared/lib/cn'
import { dateLong, dayGroup } from '../../shared/lib/format'
import { useNow } from '../../shared/hooks/useNow'
import { Button, Card, PageHeader, Tabs } from '../../shared/ui'
import { useMarkAllRead, useMarkRead, useNotifications, useUnreadCount } from './api'

const FILTERS = [
  { id: 'all' as const, label: 'Todas' },
  { id: 'unread' as const, label: 'No leídas' },
]
const GROUPS = ['Hoy', 'Ayer', 'Anteriores'] as const

export function NotificationsPage() {
  const navigate = useNavigate()
  const now = useNow()
  const { data = [], isLoading } = useNotifications()
  const unread = useUnreadCount()
  const markRead = useMarkRead()
  const markAll = useMarkAllRead()
  const [filter, setFilter] = useState<'all' | 'unread'>('all')

  const visible = data.filter((n) => filter === 'all' || !n.read)
  const groups = GROUPS.map((label) => ({ label, items: visible.filter((n) => dayGroup(n.at, now) === label) })).filter(
    (g) => g.items.length,
  )

  return (
    <>
      <PageHeader
        title="Notificaciones"
        subtitle={`${unread} sin leer`}
        actions={<Button onClick={() => markAll.mutate()}>Marcar todas como leídas</Button>}
      />
      <Tabs variant="pill" label="Filtro de notificaciones" items={FILTERS} value={filter} onChange={setFilter} />
      {!isLoading && groups.length === 0 && (
        <Card className="p-8 text-center text-muted">
          {filter === 'unread' ? 'No hay notificaciones sin leer.' : 'No tienes notificaciones.'}
        </Card>
      )}
      {groups.map((g) => (
        <section key={g.label}>
          <h2 className="mb-1.5 mt-1 text-xs font-semibold uppercase tracking-[.06em] text-muted">{g.label}</h2>
          <Card className="overflow-hidden">
            {g.items.map((n) => (
              <div
                key={n.id}
                className={cn('flex items-center gap-3 border-b border-line px-4 py-3', n.read ? 'bg-transparent' : 'bg-primary-soft')}
              >
                <span className={cn('size-2 shrink-0 rounded-full', n.read ? 'bg-transparent' : 'bg-primary')} />
                <button
                  type="button"
                  onClick={() => {
                    if (!n.read) markRead.mutate(n.id)
                    if (n.requestCode) navigate(`/solicitudes/${n.requestCode}`)
                  }}
                  className="min-w-0 flex-1 border-0 bg-transparent p-0 text-left text-fg"
                >
                  <span className={cn('block', n.read ? 'font-normal' : 'font-semibold')}>{n.text}</span>
                  <span className="mt-0.5 block text-xs text-muted">{dateLong(n.at)}</span>
                </button>
                {!n.read && (
                  <Button size="sm" onClick={() => markRead.mutate(n.id)}>
                    Marcar como leída
                  </Button>
                )}
              </div>
            ))}
          </Card>
        </section>
      ))}
    </>
  )
}
