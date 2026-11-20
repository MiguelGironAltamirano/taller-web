import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { describe, expect, it, vi } from 'vitest'
import type { Role, SessionUser } from '../../shared/domain'
import { ToastProvider } from '../../shared/ui'
import { BOTTOM_NAV, NAV } from '../navigation'
import { BottomNav } from './MobileNav'
import { Topbar } from './Topbar'

const USER: SessionUser = { id: 1, name: 'Lucía Torres', email: 'lucia@unmsm.edu.pe', role: 'solicitante' } as SessionUser
const ROLES: Role[] = ['solicitante', 'tecnico', 'supervisor', 'administrador']

function renderTopbar({ unread = 0, open = false, pathname = '/inicio' } = {}) {
  const onToggleNotifications = vi.fn()
  render(
    <MemoryRouter>
      <ToastProvider>
        <Topbar
          user={USER}
          role="solicitante"
          pathname={pathname}
          unread={unread}
          notificationsOpen={open}
          bellRef={{ current: null }}
          onToggleSidebar={vi.fn()}
          onOpenPalette={vi.fn()}
          onOpenDrawer={vi.fn()}
          onToggleNotifications={onToggleNotifications}
          onProfile={vi.fn()}
        />
      </ToastProvider>
    </MemoryRouter>,
  )
  return { onToggleNotifications }
}

describe('Topbar bell', () => {
  it('announces the unread count and that it opens a panel', () => {
    renderTopbar({ unread: 3 })
    const bell = screen.getByRole('button', { name: 'Notificaciones, 3 sin leer' })
    expect(bell).toHaveAttribute('aria-haspopup', 'dialog')
    expect(bell).toHaveAttribute('aria-expanded', 'false')
  })

  it('has a plain name when everything is read', () => {
    renderTopbar({ unread: 0 })
    expect(screen.getByRole('button', { name: 'Notificaciones' })).toBeInTheDocument()
  })

  it('reports that the panel is open', () => {
    renderTopbar({ unread: 1, open: true })
    expect(screen.getByRole('button', { name: 'Notificaciones, 1 sin leer' })).toHaveAttribute('aria-expanded', 'true')
  })

  it('opens the panel instead of navigating', async () => {
    const { onToggleNotifications } = renderTopbar({ unread: 2 })
    screen.getByRole('button', { name: /^Notificaciones/ }).click()
    expect(onToggleNotifications).toHaveBeenCalledTimes(1)
  })

  it('shows the single numeric badge of the app', () => {
    renderTopbar({ unread: 7 })
    expect(screen.getByText('7')).toBeInTheDocument()
  })

  it('looks active on the notifications page', () => {
    renderTopbar({ pathname: '/notificaciones' })
    expect(screen.getByRole('button', { name: 'Notificaciones' })).toHaveClass('bg-primary-soft')
  })
})

describe('Bottom navigation', () => {
  it.each(ROLES)('has no «Avisos» tab and no counters for %s', (role) => {
    render(
      <MemoryRouter>
        <BottomNav role={role} onMenu={vi.fn()} />
      </MemoryRouter>,
    )
    const nav = screen.getByRole('navigation', { name: 'Navegación inferior' })
    expect(nav).not.toHaveTextContent('Avisos')
    expect(nav.querySelector('a[href="/notificaciones"]')).toBeNull()
    expect(nav.textContent).not.toMatch(/\d/)
    expect(screen.getByRole('button', { name: 'Menú' })).toBeInTheDocument()
  })

  it('keeps Inicio, Solicitudes, Nueva and Menú for the requester', () => {
    render(
      <MemoryRouter>
        <BottomNav role="solicitante" onMenu={vi.fn()} />
      </MemoryRouter>,
    )
    expect(screen.getAllByRole('link').map((l) => l.textContent)).toEqual(['Inicio', 'Solicitudes', 'Nueva'])
    expect(screen.getByRole('button', { name: 'Menú' })).toBeInTheDocument()
  })
})

describe('Navigation destinations', () => {
  it.each(ROLES)('does not repeat the notifications page as a tab or a menu item for %s', (role) => {
    expect(NAV[role].some((n) => n.to === '/notificaciones')).toBe(false)
    expect(BOTTOM_NAV[role].some((n) => n.to === '/notificaciones')).toBe(false)
  })
})
