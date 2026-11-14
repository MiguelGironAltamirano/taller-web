import { createContext, useContext } from 'react'

export type ToastOptions = {
  bad?: boolean
  tone?: 'success' | 'error' | 'info'
  /** Título propio; si no, se usa el del tono ("Listo", "Aviso"...). */
  title?: string
}
export type ToastApi = { toast: (message: string, options?: ToastOptions) => void }

export const ToastContext = createContext<ToastApi | null>(null)

export function useToast(): ToastApi['toast'] {
  const ctx = useContext(ToastContext)
  if (!ctx) throw new Error('useToast debe usarse dentro de <ToastProvider>')
  return ctx.toast
}
