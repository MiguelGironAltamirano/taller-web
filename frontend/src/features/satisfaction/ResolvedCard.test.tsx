import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import type { RequestDetailView } from '../../shared/domain'
import { dateLong } from '../../shared/lib/format'
import { ToastProvider } from '../../shared/ui'
import { ResolvedCard } from './ResolvedCard'

const resolved: RequestDetailView = {
  code: 'SOL-2026-000001',
  createdAt: Date.parse('2026-10-17T10:00:00Z'),
  dueAt: Date.parse('2026-10-18T10:00:00Z'),
  state: 'Resuelta',
  category: 'Electricidad',
  priority: 'Alta',
  faculty: 'Sistemas',
  pavilion: 'Pabellón Antiguo',
  room: 'Aula 101',
  description: 'Problema eléctrico',
  requester: 'Ana Solicitante',
  area: 'Mantenimiento',
  technician: 'Carlos Técnico',
  areaId: 1,
  requesterId: 10,
  technicianId: 3,
  history: [],
  comments: [],
  evidences: [],
  solution: 'Se cambió el tomacorriente',
  rating: null,
  autoCloseAt: Date.parse('2026-10-20T15:30:00Z'),
  allowedActions: [],
  closureReason: null,
}

function renderCard(request: RequestDetailView) {
  return render(
    <QueryClientProvider client={new QueryClient()}>
      <ToastProvider>
        <ResolvedCard request={request} />
      </ToastProvider>
    </QueryClientProvider>,
  )
}

describe('ResolvedCard', () => {
  it('muestra la fecha real del cierre automático que envía el backend', () => {
    renderCard(resolved)

    expect(
      screen.getByText(`Si no respondes antes del ${dateLong(resolved.autoCloseAt!)}, la solicitud se cerrará automáticamente.`),
    ).toBeInTheDocument()
    expect(screen.queryByText(/72 horas/)).not.toBeInTheDocument()
  })

  it('sin plazo conocido no inventa un número de horas', () => {
    renderCard({ ...resolved, autoCloseAt: null })

    expect(screen.getByText('Si no respondes, la solicitud se cerrará automáticamente.')).toBeInTheDocument()
  })
})

describe('ResolvedCard accesibilidad', () => {
  it('es una región con nombre, para llegar a ella con la navegación por regiones', () => {
    renderCard(resolved)

    expect(screen.getByRole('region', { name: '¿Quedó resuelto?' })).toBeInTheDocument()
  })
})
