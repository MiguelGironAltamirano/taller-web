import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { useState } from 'react'
import { describe, expect, it, vi } from 'vitest'
import { Field, Input, PasswordInput, Select, Switch, Textarea } from './fields'

describe('Field', () => {
  it('asocia la etiqueta al control', () => {
    render(<Field label="Correo">{(a) => <Input {...a} />}</Field>)
    expect(screen.getByLabelText('Correo')).toBeInstanceOf(HTMLInputElement)
  })

  it('la ayuda es la descripción accesible y el control no queda inválido', () => {
    render(
      <Field label="Contraseña" hint="Al menos 8 caracteres.">
        {(a) => <Input {...a} />}
      </Field>,
    )
    const input = screen.getByLabelText('Contraseña')
    expect(input).toHaveAccessibleDescription('Al menos 8 caracteres.')
    expect(input).toHaveAttribute('aria-invalid', 'false')
  })

  it('el error reemplaza a la ayuda, se anuncia como descripción y marca el control como inválido', () => {
    render(
      <Field label="Contraseña" hint="Al menos 8 caracteres." error="La contraseña debe tener al menos 8 caracteres.">
        {(a) => <Input {...a} />}
      </Field>,
    )
    const input = screen.getByLabelText('Contraseña')
    expect(input).toHaveAttribute('aria-invalid', 'true')
    expect(input).toHaveAccessibleDescription('La contraseña debe tener al menos 8 caracteres.')
    expect(screen.queryByText('Al menos 8 caracteres.')).not.toBeInTheDocument()
  })

  it('sin ayuda ni error no apunta a ninguna descripción', () => {
    render(<Field label="Nombre">{(a) => <Input {...a} />}</Field>)
    expect(screen.getByLabelText('Nombre')).not.toHaveAttribute('aria-describedby')
  })

  it('cada campo recibe foco con el teclado, en orden', async () => {
    render(
      <>
        <Field label="Nombre">{(a) => <Input {...a} />}</Field>
        <Field label="Correo">{(a) => <Input {...a} />}</Field>
      </>,
    )
    await userEvent.tab()
    expect(screen.getByLabelText('Nombre')).toHaveFocus()
    await userEvent.tab()
    expect(screen.getByLabelText('Correo')).toHaveFocus()
  })
})

describe('Input', () => {
  it('con ícono sigue siendo un campo etiquetado y editable', async () => {
    render(<Field label="Correo">{(a) => <Input {...a} icon="mail" />}</Field>)
    const input = screen.getByLabelText('Correo')
    await userEvent.type(input, 'ana@unmsm.edu.pe')
    expect(input).toHaveValue('ana@unmsm.edu.pe')
  })

  it('deshabilitado no se puede editar', async () => {
    render(<Input aria-label="Código" disabled />)
    const input = screen.getByLabelText('Código')
    await userEvent.type(input, 'x')
    expect(input).toHaveValue('')
  })
})

describe('PasswordInput', () => {
  it('oculta lo escrito y permite mostrarlo y volver a ocultarlo', async () => {
    render(<Field label="Contraseña">{(a) => <PasswordInput {...a} />}</Field>)
    const input = screen.getByLabelText('Contraseña')
    await userEvent.type(input, 'Clave123')
    expect(input).toHaveAttribute('type', 'password')

    const toggle = screen.getByRole('button', { name: 'Mostrar caracteres' })
    expect(toggle).toHaveAttribute('aria-pressed', 'false')
    await userEvent.click(toggle)
    expect(input).toHaveAttribute('type', 'text')
    expect(input).toHaveValue('Clave123')

    const hide = screen.getByRole('button', { name: 'Ocultar caracteres' })
    expect(hide).toHaveAttribute('aria-pressed', 'true')
    await userEvent.click(hide)
    expect(input).toHaveAttribute('type', 'password')
  })

  it('el botón no envía el formulario', async () => {
    const onSubmit = vi.fn((e: React.FormEvent) => e.preventDefault())
    render(
      <form onSubmit={onSubmit}>
        <PasswordInput aria-label="Contraseña" />
      </form>,
    )
    await userEvent.click(screen.getByRole('button', { name: 'Mostrar caracteres' }))
    expect(onSubmit).not.toHaveBeenCalled()
  })

  it('el botón no se confunde con la etiqueta del campo', () => {
    render(<Field label="Contraseña">{(a) => <PasswordInput {...a} />}</Field>)
    expect(screen.getAllByLabelText('Contraseña')).toHaveLength(1)
  })
})

describe('Select', () => {
  it('es un desplegable nativo etiquetado que cambia de valor', async () => {
    const onChange = vi.fn()
    render(
      <Field label="Prioridad">
        {(a) => (
          <Select {...a} defaultValue="" onChange={(e) => onChange(e.target.value)}>
            <option value="">Toda prioridad</option>
            <option value="Alta">Alta</option>
            <option value="Baja">Baja</option>
          </Select>
        )}
      </Field>,
    )
    const select = screen.getByLabelText('Prioridad')
    await userEvent.selectOptions(select, 'Alta')
    expect(select).toHaveValue('Alta')
    expect(onChange).toHaveBeenCalledWith('Alta')
  })

  it('el error lo marca como inválido', () => {
    render(
      <Field label="Categoría" error="Selecciona una categoría.">
        {(a) => (
          <Select {...a}>
            <option value="">Selecciona</option>
          </Select>
        )}
      </Field>,
    )
    const select = screen.getByLabelText('Categoría')
    expect(select).toHaveAttribute('aria-invalid', 'true')
    expect(select).toHaveAccessibleDescription('Selecciona una categoría.')
  })
})

describe('Textarea', () => {
  it('es un área de texto etiquetada y respeta maxLength', async () => {
    render(<Field label="Descripción">{(a) => <Textarea {...a} maxLength={5} />}</Field>)
    const area = screen.getByLabelText('Descripción')
    await userEvent.type(area, 'abcdefgh')
    expect(area).toHaveValue('abcde')
  })
})

describe('Switch', () => {
  function Harness({ onChange }: { onChange?: (v: boolean) => void }) {
    const [on, setOn] = useState(false)
    return (
      <Switch
        label="Notificaciones por correo"
        checked={on}
        onChange={(v) => {
          setOn(v)
          onChange?.(v)
        }}
      />
    )
  }

  it('es un interruptor accesible que cambia con el clic', async () => {
    render(<Harness />)
    const toggle = screen.getByRole('switch', { name: 'Notificaciones por correo' })
    expect(toggle).toHaveAttribute('aria-checked', 'false')
    await userEvent.click(toggle)
    expect(toggle).toHaveAttribute('aria-checked', 'true')
  })

  it('cambia con el teclado (Espacio y Enter)', async () => {
    const onChange = vi.fn()
    render(<Harness onChange={onChange} />)
    screen.getByRole('switch').focus()
    await userEvent.keyboard(' ')
    await userEvent.keyboard('{Enter}')
    expect(onChange.mock.calls).toEqual([[true], [false]])
  })
})
