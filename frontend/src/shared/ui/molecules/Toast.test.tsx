import { act, fireEvent, render, screen } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { ToastProvider } from './Toast'
import { useToast, type ToastOptions } from './toast-context'

/** Botón que dispara un aviso; el aviso se lanza fuera del flujo de usuario para controlar el tiempo. */
function Trigger({ message, options }: { message: string; options?: ToastOptions }) {
  const toast = useToast()
  return <button onClick={() => toast(message, options)}>Lanzar</button>
}

function renderToast(message: string, options?: ToastOptions) {
  render(
    <ToastProvider>
      <Trigger message={message} options={options} />
    </ToastProvider>,
  )
  fireEvent.click(screen.getByRole('button', { name: 'Lanzar' }))
}

describe('useToast', () => {
  it('fuera de ToastProvider avisa del error de uso', () => {
    vi.spyOn(console, 'error').mockImplementation(() => {})
    expect(() => render(<Trigger message="x" />)).toThrow('useToast debe usarse dentro de <ToastProvider>')
  })
})

describe('ToastProvider', () => {
  beforeEach(() => vi.useFakeTimers())
  afterEach(() => vi.useRealTimers())

  it('un aviso de éxito se anuncia sin interrumpir, con su título', () => {
    renderToast('Código copiado')
    const status = screen.getByRole('status')
    expect(status).toHaveTextContent('Listo')
    expect(status).toHaveTextContent('Código copiado')
  })

  it('un error se anuncia de inmediato (role="alert")', () => {
    renderToast('No pudimos guardar.', { bad: true })
    const alert = screen.getByRole('alert')
    expect(alert).toHaveTextContent('No se pudo completar')
    expect(alert).toHaveTextContent('No pudimos guardar.')
  })

  it('acepta un título propio', () => {
    renderToast('Iniciaste sesión correctamente.', { title: 'Hola, Ana' })
    expect(screen.getByRole('status')).toHaveTextContent('Hola, Ana')
  })

  it.each([
    ['éxito', undefined, 4000],
    ['información', { tone: 'info' as const }, 5000],
    ['error', { bad: true }, 7000],
  ])('un aviso de %s se lee con calma: dura %s ms', (_, options, duration) => {
    renderToast('Mensaje', options)
    act(() => vi.advanceTimersByTime(duration - 1))
    expect(screen.getByText('Mensaje')).toBeInTheDocument()
    act(() => vi.advanceTimersByTime(1))
    expect(screen.queryByText('Mensaje')).not.toBeInTheDocument()
  })

  it('se puede cerrar antes de tiempo', () => {
    renderToast('Código copiado')
    fireEvent.click(screen.getByRole('button', { name: 'Cerrar aviso' }))
    expect(screen.queryByText('Código copiado')).not.toBeInTheDocument()
  })

  it('se pausa mientras el mouse está encima y continúa al salir', () => {
    renderToast('Código copiado')
    const toast = screen.getByRole('status')

    act(() => vi.advanceTimersByTime(3000))
    fireEvent.mouseEnter(toast)
    act(() => vi.advanceTimersByTime(10_000))
    expect(screen.getByText('Código copiado')).toBeInTheDocument()

    fireEvent.mouseLeave(toast)
    act(() => vi.advanceTimersByTime(1200))
    expect(screen.queryByText('Código copiado')).not.toBeInTheDocument()
  })

  it('se pausa también con el foco del teclado', () => {
    renderToast('Código copiado')
    fireEvent.focus(screen.getByRole('button', { name: 'Cerrar aviso' }))
    act(() => vi.advanceTimersByTime(10_000))
    expect(screen.getByText('Código copiado')).toBeInTheDocument()
  })

  it('un aviso nuevo reemplaza al anterior y reinicia su tiempo', () => {
    render(
      <ToastProvider>
        <Trigger message="Primero" />
      </ToastProvider>,
    )
    fireEvent.click(screen.getByRole('button', { name: 'Lanzar' }))
    act(() => vi.advanceTimersByTime(3000))
    fireEvent.click(screen.getByRole('button', { name: 'Lanzar' }))
    act(() => vi.advanceTimersByTime(3000))
    expect(screen.getAllByText('Primero')).toHaveLength(1)
    expect(screen.getByText('Primero')).toBeInTheDocument()
  })
})
