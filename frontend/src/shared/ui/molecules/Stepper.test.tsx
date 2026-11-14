import { render, screen, within } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { REQUEST_STATES } from '../../domain'
import { stateIcon } from '../atoms/state-icon'
import { Stepper } from './Stepper'

describe('Stepper', () => {
  it('es una lista ordenada con nombre y una etapa por estado', () => {
    render(<Stepper steps={REQUEST_STATES} current={3} label="Progreso de la solicitud" />)
    const list = screen.getByRole('list', { name: 'Progreso de la solicitud' })
    expect(list.tagName).toBe('OL')
    expect(within(list).getAllByRole('listitem')).toHaveLength(REQUEST_STATES.length)
  })

  it('marca la etapa actual con aria-current="step"', () => {
    render(<Stepper steps={REQUEST_STATES} current={3} />)
    const current = screen.getAllByRole('listitem').filter((li) => li.getAttribute('aria-current') === 'step')
    expect(current).toHaveLength(1)
    expect(current[0]).toHaveTextContent('En atención')
  })

  it('anuncia en texto el estado de cada etapa (no depende del color)', () => {
    render(<Stepper steps={REQUEST_STATES} current={2} />)
    const items = screen.getAllByRole('listitem')
    expect(items[0]).toHaveTextContent('Registrada, completado')
    expect(items[1]).toHaveTextContent('En evaluación, completado')
    expect(items[2]).toHaveTextContent('Asignada, actual')
    expect(items[3]).toHaveTextContent('En atención, pendiente')
  })

  it('muestra la fecha de cada etapa alcanzada y "Etapa actual" si no tiene', () => {
    render(
      <Stepper
        steps={REQUEST_STATES}
        current={2}
        icons={REQUEST_STATES.map(stateIcon)}
        captions={['09 nov, 19:48', '10 nov, 20:48']}
      />,
    )
    const items = screen.getAllByRole('listitem')
    expect(items[0]).toHaveTextContent('09 nov, 19:48')
    expect(items[1]).toHaveTextContent('10 nov, 20:48')
    expect(items[2]).toHaveTextContent('Etapa actual')
    expect(items[3]).not.toHaveTextContent('Etapa actual')
  })

  it('con la última etapa como actual no quedan etapas pendientes', () => {
    render(<Stepper steps={REQUEST_STATES} current={REQUEST_STATES.length - 1} />)
    expect(screen.queryByText(/, pendiente/)).not.toBeInTheDocument()
  })
})
