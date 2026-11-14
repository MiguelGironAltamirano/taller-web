import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { Alert, EmptyState, ErrorState, ProgressBar, SkeletonRows } from './feedback'

describe('SkeletonRows', () => {
  it('anuncia que está cargando y dibuja las filas pedidas', () => {
    const { container } = render(<SkeletonRows count={3} />)
    expect(screen.getByRole('status', { name: 'Cargando' })).toBeInTheDocument()
    expect(container.querySelectorAll('[aria-hidden="true"]')).toHaveLength(3)
  })
})

describe('EmptyState', () => {
  it('muestra el título, la descripción y la acción', async () => {
    const onClick = vi.fn()
    render(
      <EmptyState
        title="Sin resultados"
        description="Ninguna solicitud coincide con los filtros."
        action={<button onClick={onClick}>Limpiar filtros</button>}
      />,
    )
    expect(screen.getByText('Sin resultados')).toBeInTheDocument()
    expect(screen.getByText('Ninguna solicitud coincide con los filtros.')).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Limpiar filtros' }))
    expect(onClick).toHaveBeenCalledOnce()
  })

  it('no es una alerta: un estado vacío no es un error', () => {
    render(<EmptyState title="Sin resultados" />)
    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
  })
})

describe('ErrorState', () => {
  it('se anuncia como alerta y ofrece reintentar', async () => {
    const onRetry = vi.fn()
    render(<ErrorState title="No pudimos cargar las solicitudes" description="Revisa tu conexión." onRetry={onRetry} />)

    const alert = screen.getByRole('alert')
    expect(alert).toHaveTextContent('No pudimos cargar las solicitudes')
    expect(alert).toHaveTextContent('Revisa tu conexión.')

    await userEvent.click(screen.getByRole('button', { name: 'Reintentar' }))
    expect(onRetry).toHaveBeenCalledOnce()
  })

  it('mientras reintenta, el botón queda ocupado y no repite la petición', async () => {
    const onRetry = vi.fn()
    render(<ErrorState title="No pudimos cargar la solicitud" onRetry={onRetry} retrying />)

    const button = screen.getByRole('button', { name: 'Reintentar' })
    expect(button).toBeDisabled()
    expect(button).toHaveAttribute('aria-busy', 'true')
    await userEvent.click(button)
    expect(onRetry).not.toHaveBeenCalled()
  })
})

describe('Alert', () => {
  it('un error se anuncia de inmediato (role="alert")', () => {
    render(<Alert>Credenciales inválidas.</Alert>)
    expect(screen.getByRole('alert')).toHaveTextContent('Credenciales inválidas.')
  })

  it.each(['info', 'success', 'warn'] as const)('el tono "%s" se anuncia sin interrumpir (role="status")', (tone) => {
    render(<Alert tone={tone}>Mensaje</Alert>)
    expect(screen.getByRole('status')).toHaveTextContent('Mensaje')
    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
  })

  it('el ícono es decorativo', () => {
    const { container } = render(<Alert tone="warn">Atención</Alert>)
    expect(container.querySelector('svg')).toHaveAttribute('aria-hidden', 'true')
  })
})

describe('ProgressBar', () => {
  it('expone el valor, sus límites y su nombre', () => {
    render(<ProgressBar value={62.4} label="Plazo de atención consumido" />)
    const bar = screen.getByRole('progressbar', { name: 'Plazo de atención consumido' })
    expect(bar).toHaveAttribute('aria-valuenow', '62')
    expect(bar).toHaveAttribute('aria-valuemin', '0')
    expect(bar).toHaveAttribute('aria-valuemax', '100')
  })

  it.each([
    [-20, '0'],
    [140, '100'],
  ])('acota %s al rango 0–100', (value, expected) => {
    render(<ProgressBar value={value} label="Carga" />)
    expect(screen.getByRole('progressbar')).toHaveAttribute('aria-valuenow', expected)
  })
})
