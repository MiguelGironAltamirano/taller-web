import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { Card, CardTitle, KpiCard, PageHeader, SectionIcon } from './layout'

describe('PageHeader', () => {
  it('el título es el único h1 de la página', () => {
    render(<PageHeader title="Bandeja del área" subtitle="9 de 9 solicitudes" />)
    expect(screen.getByRole('heading', { level: 1, name: 'Bandeja del área' })).toBeInTheDocument()
    expect(screen.getAllByRole('heading', { level: 1 })).toHaveLength(1)
    expect(screen.getByText('9 de 9 solicitudes')).toBeInTheDocument()
  })

  it('el ícono de sección es decorativo y no forma parte del título', () => {
    const { container } = render(<PageHeader icon="file" title="Bandeja del área" />)
    expect(container.querySelector('[aria-hidden="true"] svg, svg[aria-hidden="true"]')).not.toBeNull()
    expect(screen.getByRole('heading', { level: 1 })).toHaveAccessibleName('Bandeja del área')
  })

  it('muestra las acciones', async () => {
    const onExport = vi.fn()
    render(<PageHeader title="Reportes" actions={<button onClick={onExport}>Exportar</button>} />)
    await userEvent.click(screen.getByRole('button', { name: 'Exportar' }))
    expect(onExport).toHaveBeenCalledOnce()
  })
})

describe('SectionIcon', () => {
  it('se oculta a los lectores de pantalla', () => {
    const { container } = render(<SectionIcon name="bell" />)
    expect(container.firstElementChild).toHaveAttribute('aria-hidden', 'true')
  })
})

describe('Card y CardTitle', () => {
  it('Card es una sección y CardTitle un h2', () => {
    render(
      <Card aria-label="Resumen">
        <CardTitle>Por prioridad</CardTitle>
      </Card>,
    )
    expect(screen.getByRole('region', { name: 'Resumen' })).toBeInTheDocument()
    expect(screen.getByRole('heading', { level: 2, name: 'Por prioridad' })).toBeInTheDocument()
  })
})

describe('KpiCard', () => {
  it('sin acción es un grupo con el nombre del indicador, su valor y su variación', () => {
    render(
      <KpiCard
        label="Pendientes"
        value={7}
        icon="clock"
        trend={{ text: '12% vs. periodo anterior', tone: 'good', direction: 'down' }}
      />,
    )
    const group = screen.getByRole('group', { name: 'Pendientes' })
    expect(group).toHaveTextContent('7')
    expect(group).toHaveTextContent('12% vs. periodo anterior')
    expect(screen.queryByRole('button')).not.toBeInTheDocument()
  })

  it('con acción es un botón que lleva al detalle', async () => {
    const onClick = vi.fn()
    render(<KpiCard label="Vencidas" value={3} onClick={onClick} />)
    const button = screen.getByRole('button', { name: /Vencidas/ })
    expect(button).toHaveTextContent('3')
    await userEvent.click(button)
    expect(onClick).toHaveBeenCalledOnce()
  })

  it('la flecha de la variación es decorativa: el texto dice la cifra', () => {
    const { container } = render(
      <KpiCard label="Atendidas" value={2} trend={{ text: '15% vs. periodo anterior', tone: 'good', direction: 'up' }} />,
    )
    expect(container.querySelector('[aria-hidden="true"]')).toHaveTextContent('↑')
  })
})
