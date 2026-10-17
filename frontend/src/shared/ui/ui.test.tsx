import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { useState } from 'react'
import { describe, expect, it, vi } from 'vitest'
import { Button, Field, Input, Modal, Pagination, PriorityBadge, StarRating, StatusBadge, Tabs } from '.'

describe('Button', () => {
  it('queda deshabilitado y marcado como ocupado mientras carga', () => {
    render(<Button loading>Guardar</Button>)
    const btn = screen.getByRole('button', { name: 'Guardar' })
    expect(btn).toBeDisabled()
    expect(btn).toHaveAttribute('aria-busy', 'true')
  })
})

describe('Badges', () => {
  it('muestran siempre texto, no solo color', () => {
    render(
      <>
        <StatusBadge state="En atención" />
        <PriorityBadge priority="Crítica" />
      </>,
    )
    expect(screen.getByText('En atención')).toBeInTheDocument()
    expect(screen.getByText('Crítica')).toBeInTheDocument()
  })
})

describe('Field', () => {
  it('asocia la etiqueta y el mensaje de error al control', () => {
    render(
      <Field label="Correo" error="Ingresa tu correo.">
        {(a) => <Input {...a} />}
      </Field>,
    )
    const input = screen.getByLabelText('Correo')
    expect(input).toHaveAttribute('aria-invalid', 'true')
    expect(input).toHaveAccessibleDescription('Ingresa tu correo.')
  })
})

describe('Modal', () => {
  it('se cierra con Escape y tiene rol de diálogo', async () => {
    const onClose = vi.fn()
    render(
      <Modal open onClose={onClose} title="Cambiar contraseña">
        <input aria-label="Actual" />
      </Modal>,
    )
    expect(screen.getByRole('dialog', { name: 'Cambiar contraseña' })).toBeInTheDocument()
    await userEvent.keyboard('{Escape}')
    expect(onClose).toHaveBeenCalledOnce()
  })

  it('no renderiza nada cuando está cerrado', () => {
    render(
      <Modal open={false} onClose={() => {}} title="X">
        contenido
      </Modal>,
    )
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
  })
})

describe('StarRating', () => {
  it('permite calificar de 1 a 5', async () => {
    function Harness() {
      const [v, setV] = useState(0)
      return (
        <>
          <StarRating value={v} onChange={setV} />
          <output>{v}</output>
        </>
      )
    }
    render(<Harness />)
    await userEvent.click(screen.getByRole('radio', { name: '4 estrellas' }))
    expect(screen.getByRole('radio', { name: '4 estrellas' })).toBeChecked()
    expect(screen.getByRole('status')).toHaveTextContent('4')
  })
})

describe('Tabs', () => {
  it('marca la pestaña activa y notifica el cambio', async () => {
    const onChange = vi.fn()
    render(
      <Tabs
        label="Secciones"
        value="a"
        onChange={onChange}
        items={[
          { id: 'a', label: 'Resumen' },
          { id: 'b', label: 'Historial', count: 3 },
        ]}
      />,
    )
    expect(screen.getByRole('tab', { name: 'Resumen' })).toHaveAttribute('aria-selected', 'true')
    await userEvent.click(screen.getByRole('tab', { name: /Historial/ }))
    expect(onChange).toHaveBeenCalledWith('b')
  })
})

describe('Pagination', () => {
  it('muestra el rango y deshabilita Anterior en la primera página', () => {
    render(<Pagination page={0} pageSize={10} total={23} onPage={() => {}} onPageSize={() => {}} />)
    expect(screen.getByText('1–10 de 23')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Anterior' })).toBeDisabled()
    expect(screen.getByRole('button', { name: 'Siguiente' })).toBeEnabled()
  })
})
