import { useId, useState, type ComponentProps, type ReactNode } from 'react'
import { cn } from '../../lib/cn'
import { controlClass } from '../../lib/controlClass'
import { Icon } from './Icon'

type FieldProps = {
  label: string
  error?: string
  hint?: ReactNode
  /** Render-prop que recibe el id y los atributos ARIA del control. */
  children: (a: { id: string; 'aria-invalid': boolean; 'aria-describedby'?: string }) => ReactNode
  className?: string
}

/** Etiqueta + control + mensaje de error accesible. */
export function Field({ label, error, hint, children, className }: FieldProps) {
  const id = useId()
  const descId = `${id}-desc`
  const hasDesc = Boolean(error || hint)
  return (
    <div className={cn('flex flex-col gap-1.5 text-[13px] font-medium', className)}>
      <label htmlFor={id}>{label}</label>
      {children({ id, 'aria-invalid': Boolean(error), 'aria-describedby': hasDesc ? descId : undefined })}
      {hasDesc && (
        <span
          id={descId}
          className={cn('flex items-start gap-1.5 text-[12.5px] font-normal leading-snug', error ? 'text-err-fg' : 'text-muted')}
        >
          <Icon name={error ? 'alert' : 'help'} size={14} strokeWidth={2} className="mt-px shrink-0" />
          <span>{error ?? hint}</span>
        </span>
      )}
    </div>
  )
}

type PasswordInputProps = Omit<InputProps, 'type'>

/** Campo de contraseña con botón para mostrar u ocultar lo escrito. */
export function PasswordInput({ className, ...rest }: PasswordInputProps) {
  const [visible, setVisible] = useState(false)
  return (
    <span className="relative block">
      <Input {...rest} type={visible ? 'text' : 'password'} className={cn('pr-11', className)} />
      <button
        type="button"
        onClick={() => setVisible((v) => !v)}
        aria-pressed={visible}
        aria-label={visible ? 'Ocultar caracteres' : 'Mostrar caracteres'}
        title={visible ? 'Ocultar caracteres' : 'Mostrar caracteres'}
        className="absolute inset-y-0 right-1 my-auto grid size-8 place-items-center rounded-md border-0 bg-transparent text-muted hover:bg-surface2 hover:text-fg"
      >
        <Icon name={visible ? 'eyeOff' : 'eye'} size={17} />
      </button>
    </span>
  )
}

const hasHeight = (className?: string) => /(^|\s)h-/.test(className ?? '')

type InputProps = ComponentProps<'input'> & { invalid?: boolean }

export function Input({ invalid, className, ...rest }: InputProps) {
  return <input className={controlClass(invalid, cn(!hasHeight(className) && 'h-10', 'px-3', className))} {...rest} />
}

type SelectProps = ComponentProps<'select'> & { invalid?: boolean }

export function Select({ invalid, className, children, ...rest }: SelectProps) {
  return (
    <select
      className={controlClass(invalid, cn('ui-select', !hasHeight(className) && 'h-10', 'pl-3', className))}
      {...rest}
    >
      {children}
    </select>
  )
}

type TextareaProps = ComponentProps<'textarea'> & { invalid?: boolean }

export function Textarea({ invalid, className, ...rest }: TextareaProps) {
  return <textarea className={controlClass(invalid, cn('resize-y p-2.5', className))} {...rest} />
}

type SwitchProps = { checked: boolean; onChange: (next: boolean) => void; label: string }

/** Interruptor accesible (role="switch"). */
export function Switch({ checked, onChange, label }: SwitchProps) {
  return (
    <button
      type="button"
      role="switch"
      aria-checked={checked}
      aria-label={label}
      onClick={() => onChange(!checked)}
      className={cn(
        'relative h-[22px] w-[38px] shrink-0 rounded-full border-0 transition-colors',
        checked ? 'bg-primary' : 'bg-line',
      )}
    >
      <span
        className={cn(
          'absolute top-0.5 size-[18px] rounded-full bg-white transition-[left]',
          checked ? 'left-[18px]' : 'left-0.5',
        )}
      />
    </button>
  )
}
