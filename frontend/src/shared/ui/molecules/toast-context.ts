import { createContext, useContext } from 'react'

export type ToastApi = { toast: (message: string, options?: { bad?: boolean }) => void }

export const ToastContext = createContext<ToastApi | null>(null)

export function useToast(): ToastApi['toast'] {
  const ctx = useContext(ToastContext)
  if (!ctx) throw new Error('useToast debe usarse dentro de <ToastProvider>')
  return ctx.toast
}
