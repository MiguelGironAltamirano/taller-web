import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { Button, IconButton } from './Button'
import { Icon } from './Icon'

describe('Button', () => {
  it('es de tipo "button" por defecto para no enviar formularios sin querer', () => {
    render(<Button>Guardar</Button>)
    expect(screen.getByRole('button', { name: 'Guardar' })).toHaveAttribute('type', 'button')
  })

  it('respeta type="submit" y envía el formulario', async () => {
    const onSubmit = vi.fn((e: React.FormEvent) => e.preventDefault())
    render(
      <form onSubmit={onSubmit}>
        <Button type="submit">Enviar</Button>
      </form>,
    )
    await userEvent.click(screen.getByRole('button', { name: 'Enviar' }))
    expect(onSubmit).toHaveBeenCalledOnce()
  })

  it('ejecuta la acción con el clic y con el teclado (Enter y Espacio)', async () => {
    const onClick = vi.fn()
    render(<Button onClick={onClick}>Exportar</Button>)
    const button = screen.getByRole('button', { name: 'Exportar' })

    await userEvent.click(button)
    button.focus()
    await userEvent.keyboard('{Enter}')
    await userEvent.keyboard(' ')

    expect(onClick).toHaveBeenCalledTimes(3)
  })

  it('deshabilitado no dispara la acción', async () => {
    const onClick = vi.fn()
    render(
      <Button disabled onClick={onClick}>
        Exportar
      </Button>,
    )
    const button = screen.getByRole('button', { name: 'Exportar' })

    expect(button).toBeDisabled()
    await userEvent.click(button)
    expect(onClick).not.toHaveBeenCalled()
  })

  it('mientras carga queda deshabilitado, marcado como ocupado y conserva su nombre', async () => {
    const onClick = vi.fn()
    render(
      <Button loading onClick={onClick}>
        Guardar
      </Button>,
    )
    const button = screen.getByRole('button', { name: 'Guardar' })

    expect(button).toBeDisabled()
    expect(button).toHaveAttribute('aria-busy', 'true')
    await userEvent.click(button)
    expect(onClick).not.toHaveBeenCalled()
  })

  it('no marca aria-busy cuando no está cargando', () => {
    render(<Button>Guardar</Button>)
    expect(screen.getByRole('button', { name: 'Guardar' })).not.toHaveAttribute('aria-busy')
  })

  it('un ícono dentro del botón no cambia su nombre accesible', () => {
    render(
      <Button>
        <Icon name="download" />
        Exportar
      </Button>,
    )
    expect(screen.getByRole('button', { name: 'Exportar' })).toBeInTheDocument()
  })
})

describe('IconButton', () => {
  it('toma su nombre accesible y su tooltip de la etiqueta', () => {
    render(
      <IconButton label="Copiar código">
        <Icon name="copy" />
      </IconButton>,
    )
    const button = screen.getByRole('button', { name: 'Copiar código' })
    expect(button).toHaveAttribute('title', 'Copiar código')
  })

  it('ejecuta su acción y respeta disabled', async () => {
    const onClick = vi.fn()
    const { rerender } = render(
      <IconButton label="Cerrar" onClick={onClick}>
        <Icon name="x" />
      </IconButton>,
    )
    await userEvent.click(screen.getByRole('button', { name: 'Cerrar' }))
    expect(onClick).toHaveBeenCalledOnce()

    rerender(
      <IconButton label="Cerrar" onClick={onClick} disabled>
        <Icon name="x" />
      </IconButton>,
    )
    await userEvent.click(screen.getByRole('button', { name: 'Cerrar' }))
    expect(onClick).toHaveBeenCalledOnce()
  })
})
