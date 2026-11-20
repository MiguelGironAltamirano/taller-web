import { createContext, useContext } from 'react'

export type ToastOptions = {
  /** Sin tono, el aviso es de éxito. Los errores llevan `tone: 'error'` y un texto de `errorMessage`. */
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
