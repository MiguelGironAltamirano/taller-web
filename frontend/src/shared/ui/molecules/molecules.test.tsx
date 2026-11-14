import { fireEvent, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { useState } from 'react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import type { PickedFile } from '../../lib/files'
import { FileDropzone } from './FileDropzone'
import { Modal, ModalActions } from './Modal'
import { Pagination } from './Pagination'
import { StarRating } from './StarRating'
import { Segmented, Tabs } from './Tabs'
import { Timeline } from './Timeline'

describe('Tabs', () => {
  it('la variante "pill" marca la pestaña activa y notifica el cambio', async () => {
    const onChange = vi.fn()
    render(
      <Tabs
        variant="pill"
        label="Filtro de notificaciones"
        value="all"
        onChange={onChange}
        items={[
          { id: 'all', label: 'Todas' },
          { id: 'unread', label: 'No leídas' },
        ]}
      />,
    )
    expect(screen.getByRole('tablist', { name: 'Filtro de notificaciones' })).toBeInTheDocument()
    expect(screen.getByRole('tab', { name: 'Todas' })).toHaveAttribute('aria-selected', 'true')
    await userEvent.click(screen.getByRole('tab', { name: 'No leídas' }))
    expect(onChange).toHaveBeenCalledWith('unread')
  })

  it('muestra el contador cuando lo hay (también 0) y lo omite si está vacío', () => {
    render(
      <Tabs
        label="Secciones"
        value="a"
        onChange={() => {}}
        items={[
          { id: 'a', label: 'Comentarios', count: 0 },
          { id: 'b', label: 'Evidencias', count: '' },
        ]}
      />,
    )
    expect(screen.getByRole('tab', { name: 'Comentarios 0' })).toBeInTheDocument()
    expect(screen.getByRole('tab', { name: 'Evidencias' })).toBeInTheDocument()
  })
})

describe('Segmented', () => {
  it('es un grupo con nombre y marca la opción elegida con aria-pressed', async () => {
    const onChange = vi.fn()
    render(
      <Segmented
        label="Vista"
        value="table"
        onChange={onChange}
        items={[
          { id: 'table', label: 'Tabla' },
          { id: 'kanban', label: 'Kanban' },
        ]}
      />,
    )
    expect(screen.getByRole('group', { name: 'Vista' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Tabla' })).toHaveAttribute('aria-pressed', 'true')
    expect(screen.getByRole('button', { name: 'Kanban' })).toHaveAttribute('aria-pressed', 'false')
    await userEvent.click(screen.getByRole('button', { name: 'Kanban' }))
    expect(onChange).toHaveBeenCalledWith('kanban')
  })
})

describe('Modal', () => {
  function Opener() {
    const [open, setOpen] = useState(false)
    return (
      <>
        <button onClick={() => setOpen(true)}>Cambiar contraseña</button>
        <Modal open={open} onClose={() => setOpen(false)} title="Cambiar contraseña" subtitle="Elige una nueva.">
          <input aria-label="Actual" />
          <ModalActions>
            <button onClick={() => setOpen(false)}>Cancelar</button>
          </ModalActions>
        </Modal>
      </>
    )
  }

  it('al abrir lleva el foco al primer control y al cerrar lo devuelve a quien lo abrió', async () => {
    render(<Opener />)
    const opener = screen.getByRole('button', { name: 'Cambiar contraseña' })
    await userEvent.click(opener)

    expect(screen.getByRole('dialog', { name: 'Cambiar contraseña' })).toHaveAttribute('aria-modal', 'true')
    expect(screen.getByLabelText('Actual')).toHaveFocus()
    expect(screen.getByText('Elige una nueva.')).toBeInTheDocument()

    await userEvent.keyboard('{Escape}')
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    expect(opener).toHaveFocus()
  })

  it('se cierra al hacer clic en el fondo', async () => {
    const onClose = vi.fn()
    render(
      <Modal open onClose={onClose} title="Detalle">
        contenido
      </Modal>,
    )
    const backdrop = document.querySelector('[aria-hidden="true"].fixed.inset-0') as HTMLElement
    await userEvent.click(backdrop)
    expect(onClose).toHaveBeenCalledOnce()
  })

  it('con onSubmit el diálogo es un formulario que se envía con Enter', async () => {
    const onSubmit = vi.fn((e: React.FormEvent) => e.preventDefault())
    render(
      <Modal open onClose={() => {}} title="Rechazar solución" onSubmit={onSubmit}>
        <input aria-label="Motivo" />
        <button type="submit">Rechazar</button>
      </Modal>,
    )
    expect(screen.getByRole('dialog', { name: 'Rechazar solución' }).tagName).toBe('FORM')
    await userEvent.type(screen.getByLabelText('Motivo'), 'Sigue fallando{Enter}')
    expect(onSubmit).toHaveBeenCalledOnce()
  })

  it('deja de escuchar Escape al cerrarse', async () => {
    const onClose = vi.fn()
    const { rerender } = render(
      <Modal open onClose={onClose} title="X">
        contenido
      </Modal>,
    )
    rerender(
      <Modal open={false} onClose={onClose} title="X">
        contenido
      </Modal>,
    )
    await userEvent.keyboard('{Escape}')
    expect(onClose).not.toHaveBeenCalled()
  })
})

describe('Pagination', () => {
  it('avanza a la página siguiente', async () => {
    const onPage = vi.fn()
    render(<Pagination page={0} pageSize={10} total={23} onPage={onPage} onPageSize={() => {}} />)
    await userEvent.click(screen.getByRole('button', { name: 'Siguiente' }))
    expect(onPage).toHaveBeenCalledWith(1)
  })

  it('en la última página muestra el rango final y deshabilita Siguiente', async () => {
    const onPage = vi.fn()
    render(<Pagination page={2} pageSize={10} total={23} onPage={onPage} onPageSize={() => {}} />)
    expect(screen.getByText('21–23 de 23')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Siguiente' })).toBeDisabled()
    await userEvent.click(screen.getByRole('button', { name: 'Anterior' }))
    expect(onPage).toHaveBeenCalledWith(1)
  })

  it('cambia la cantidad de filas con un desplegable etiquetado', async () => {
    const onPageSize = vi.fn()
    render(<Pagination page={0} pageSize={10} total={23} onPage={() => {}} onPageSize={onPageSize} />)
    await userEvent.selectOptions(screen.getByLabelText('Filas'), '25')
    expect(onPageSize).toHaveBeenCalledWith(25)
  })

  it('sin resultados lo dice y no permite avanzar', () => {
    render(<Pagination page={0} pageSize={10} total={0} onPage={() => {}} onPageSize={() => {}} />)
    expect(screen.getByText('0 resultados')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Siguiente' })).toBeDisabled()
  })
})

describe('StarRating', () => {
  it('editable: marca solo la estrella elegida y usa el singular en la primera', () => {
    render(<StarRating value={3} onChange={() => {}} />)
    expect(screen.getByRole('radiogroup', { name: 'Calificación' })).toBeInTheDocument()
    expect(screen.getByRole('radio', { name: '1 estrella' })).toBeInTheDocument()
    expect(screen.getByRole('radio', { name: '3 estrellas' })).toHaveAttribute('aria-checked', 'true')
    expect(screen.getAllByRole('radio').filter((r) => r.getAttribute('aria-checked') === 'true')).toHaveLength(1)
  })

  it('se puede calificar con el teclado', async () => {
    const onChange = vi.fn()
    render(<StarRating value={0} onChange={onChange} />)
    screen.getByRole('radio', { name: '5 estrellas' }).focus()
    await userEvent.keyboard('{Enter}')
    expect(onChange).toHaveBeenCalledWith(5)
  })
})

describe('Timeline', () => {
  it('es una lista ordenada con título, detalle y datos de cada paso', () => {
    render(
      <Timeline
        items={[
          { id: 1, title: 'Registrada', meta: 'Ana · 09 nov' },
          { id: 2, title: 'Resuelta', body: 'Se cambió el tomacorriente', meta: 'Carlos · 11 nov' },
        ]}
      />,
    )
    const items = screen.getAllByRole('listitem')
    expect(items).toHaveLength(2)
    expect(items[0]).toHaveTextContent('Registrada')
    expect(items[1]).toHaveTextContent('Se cambió el tomacorriente')
    expect(items[1]).toHaveTextContent('Carlos · 11 nov')
  })
})

describe('FileDropzone', () => {
  beforeEach(() => {
    URL.createObjectURL = vi.fn(() => 'blob:preview')
  })
  afterEach(() => {
    // jsdom no implementa createObjectURL; se restituye para no afectar a otras pruebas.
    delete (URL as { createObjectURL?: unknown }).createObjectURL
  })

  function Harness() {
    const [files, setFiles] = useState<PickedFile[]>([])
    return <FileDropzone files={files} onChange={setFiles} />
  }

  const file = (name: string, size = 1024, type = 'image/png') => new File([new Uint8Array(size)], name, { type })

  it('se elige desde la etiqueta y lista los archivos con su tamaño', async () => {
    render(<Harness />)
    await userEvent.upload(screen.getByLabelText(/Arrastra archivos/), [file('foco.png', 2048), file('acta.pdf', 1024, 'application/pdf')])
    expect(screen.getByText('foco.png')).toBeInTheDocument()
    expect(screen.getByText('2 KB')).toBeInTheDocument()
    expect(screen.getByText('acta.pdf')).toBeInTheDocument()
    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
  })

  it('rechaza un tipo no permitido con un mensaje que lo explica', async () => {
    render(<Harness />)
    // El input acepta solo JPG, PNG o PDF; aquí se prueba la validación aunque el navegador dejara pasar el archivo.
    fireEvent.change(screen.getByLabelText(/Arrastra archivos/), { target: { files: [file('notas.docx')] } })
    expect(await screen.findByRole('alert')).toHaveTextContent('"notas.docx": solo se permiten JPG, PNG o PDF.')
    expect(screen.queryByText('notas.docx', { selector: '.truncate' })).not.toBeInTheDocument()
  })

  it('acepta archivos soltados sobre la zona', () => {
    render(<Harness />)
    fireEvent.drop(screen.getByText(/Arrastra archivos/).closest('label')!, {
      dataTransfer: { files: [file('foto.jpg', 1024, 'image/jpeg')] },
    })
    expect(screen.getByText('foto.jpg')).toBeInTheDocument()
  })

  it('cada archivo se puede quitar con un botón que dice cuál', async () => {
    render(<Harness />)
    await userEvent.upload(screen.getByLabelText(/Arrastra archivos/), file('foco.png'))
    await userEvent.click(screen.getByRole('button', { name: 'Quitar foco.png' }))
    expect(screen.queryByText('foco.png')).not.toBeInTheDocument()
  })
})
