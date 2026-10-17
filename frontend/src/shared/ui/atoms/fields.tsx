import { useId, type ComponentProps, type ReactNode } from 'react'
import { cn } from '../../lib/cn'
import { controlClass } from '../../lib/controlClass'

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
        <span id={descId} className={cn('font-normal', error ? 'text-err-fg' : 'text-muted')}>
          {error ?? hint}
        </span>
      )}
    </div>
  )
}

type InputProps = ComponentProps<'input'> & { invalid?: boolean }

export function Input({ invalid, className, ...rest }: InputProps) {
  return <input className={controlClass(invalid, cn('h-10 px-3', className))} {...rest} />
}

type SelectProps = ComponentProps<'select'> & { invalid?: boolean }

export function Select({ invalid, className, children, ...rest }: SelectProps) {
  return (
    <select className={controlClass(invalid, cn('h-10 px-2', className))} {...rest}>
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
