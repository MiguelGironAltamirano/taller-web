import { useEffect, useRef, type ReactNode } from 'react'
import { createPortal } from 'react-dom'
import { cn } from '../../lib/cn'

type ModalProps = {
  open: boolean
  onClose: () => void
  title: string
  subtitle?: string
  children: ReactNode
  /** `center`: diálogo centrado; `drawer`: panel lateral derecho. */
  variant?: 'center' | 'drawer'
  /** Si se pasa, el contenido se envuelve en un `<form>`. */
  onSubmit?: (e: React.FormEvent<HTMLFormElement>) => void
  width?: string
}

const FOCUSABLE = 'input, select, textarea, button:not([data-modal-close]), [href]'

/** Diálogo accesible con fondo oscuro, cierre con Escape y foco inicial en el primer control. */
export function Modal({ open, onClose, title, subtitle, children, variant = 'center', onSubmit, width = 'min(440px,92%)' }: ModalProps) {
  const panelRef = useRef<HTMLElement | null>(null)

  useEffect(() => {
    if (!open) return
    const previous = document.activeElement as HTMLElement | null
    panelRef.current?.querySelector<HTMLElement>(FOCUSABLE)?.focus()
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose()
    }
    document.addEventListener('keydown', onKey)
    return () => {
      document.removeEventListener('keydown', onKey)
      previous?.focus?.()
    }
  }, [open, onClose])

  if (!open) return null

  const content = (
    <>
      <div>
        <h2 className="m-0 text-lg font-semibold">{title}</h2>
        {subtitle && <div className="mt-0.5 text-muted">{subtitle}</div>}
      </div>
      {children}
    </>
  )
  const panelClass =
    variant === 'drawer'
      ? 'fixed inset-y-0 right-0 z-[71] flex w-[min(400px,100%)] animate-pop flex-col gap-3.5 overflow-auto border-l border-line bg-surface p-5 shadow-[-12px_0_32px_rgb(0_0_0/0.2)]'
      : 'fixed left-1/2 top-1/2 z-[71] flex max-h-[90%] -translate-x-1/2 -translate-y-1/2 animate-pop flex-col gap-3 overflow-auto rounded-xl bg-surface p-5 shadow-modal'

  return createPortal(
    <>
      <div className="fixed inset-0 z-[70] bg-black/45" onClick={onClose} aria-hidden="true" />
      {onSubmit ? (
        <form
          ref={panelRef as React.Ref<HTMLFormElement>}
          role="dialog"
          aria-modal="true"
          aria-label={title}
          noValidate
          onSubmit={onSubmit}
          className={panelClass}
          style={variant === 'center' ? { width } : undefined}
        >
          {content}
        </form>
      ) : (
        <div
          ref={panelRef as React.Ref<HTMLDivElement>}
          role="dialog"
          aria-modal="true"
          aria-label={title}
          className={panelClass}
          style={variant === 'center' ? { width } : undefined}
        >
          {content}
        </div>
      )}
    </>,
    document.body,
  )
}

export function ModalActions({ children, className }: { children: ReactNode; className?: string }) {
  return <div className={cn('flex justify-end gap-2', className)}>{children}</div>
}
