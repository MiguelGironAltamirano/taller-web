import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { PRIORITIES, REQUEST_STATES } from '../../domain'
import { CountBadge, OverdueBadge, PriorityBadge, StatusBadge, ToneBadge } from './Badge'
import { stateIcon } from './state-icon'

describe('StatusBadge', () => {
  it.each(REQUEST_STATES)('muestra el estado "%s" como texto, con un ícono decorativo', (state) => {
    const { container } = render(<StatusBadge state={state} />)
    expect(screen.getByText(state)).toBeInTheDocument()
    // El ícono acompaña al texto (no lo reemplaza) y el lector de pantalla no lo lee.
    const icon = container.querySelector('svg')
    expect(icon).toHaveAttribute('aria-hidden', 'true')
  })

  it('cada estado tiene un ícono propio', () => {
    const icons = REQUEST_STATES.map(stateIcon)
    expect(new Set(icons).size).toBe(REQUEST_STATES.length)
  })
})

describe('PriorityBadge', () => {
  it.each(PRIORITIES)('muestra la prioridad "%s" como texto', (priority) => {
    const { container } = render(<PriorityBadge priority={priority} />)
    expect(screen.getByText(priority)).toBeInTheDocument()
    expect(container.querySelector('svg')).toHaveAttribute('aria-hidden', 'true')
  })

  it('el nivel se ve en las barras, no solo en el color: más prioridad, más barras llenas', () => {
    const filled = (priority: (typeof PRIORITIES)[number]) => {
      const { container, unmount } = render(<PriorityBadge priority={priority} />)
      const bars = [...container.querySelectorAll('rect')].filter((r) => r.getAttribute('opacity') === '1')
      unmount()
      return bars.length
    }
    expect(filled('Baja')).toBe(1)
    expect(filled('Media')).toBe(2)
    expect(filled('Alta')).toBe(3)
  })
})

describe('OverdueBadge', () => {
  it('dice "Vencida" con texto', () => {
    render(<OverdueBadge />)
    expect(screen.getByText('Vencida')).toBeInTheDocument()
  })
})

describe('ToneBadge y CountBadge', () => {
  it('ToneBadge muestra su contenido', () => {
    render(<ToneBadge tone="ok">Activo</ToneBadge>)
    expect(screen.getByText('Activo')).toBeInTheDocument()
  })

  it('CountBadge muestra el número', () => {
    render(<CountBadge value={12} />)
    expect(screen.getByText('12')).toBeInTheDocument()
  })
})
