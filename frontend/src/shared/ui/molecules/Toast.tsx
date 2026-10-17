import { useCallback, useEffect, useMemo, useRef, useState, type ReactNode } from 'react'
import { cn } from '../../lib/cn'
import { ToastContext, type ToastApi } from './toast-context'

type ToastState = { message: string; bad: boolean; id: number }

const DURATION_MS = 3000

export function ToastProvider({ children }: { children: ReactNode }) {
  const [current, setCurrent] = useState<ToastState | null>(null)
  const timer = useRef<ReturnType<typeof setTimeout> | null>(null)

  useEffect(() => () => void (timer.current && clearTimeout(timer.current)), [])

  const toast = useCallback<ToastApi['toast']>((message, options) => {
    if (timer.current) clearTimeout(timer.current)
    setCurrent({ message, bad: Boolean(options?.bad), id: Date.now() })
    timer.current = setTimeout(() => setCurrent(null), DURATION_MS)
  }, [])

  const api = useMemo(() => ({ toast }), [toast])

  return (
    <ToastContext.Provider value={api}>
      {children}
      {current && (
        <div
          role="status"
          key={current.id}
          className={cn(
            'fixed bottom-[76px] left-1/2 z-[90] max-w-[92%] -translate-x-1/2 animate-pop rounded-[10px] px-4 py-2.5 text-[13px] font-medium text-white shadow-[0_8px_24px_rgb(0_0_0/0.25)] md:bottom-5',
            current.bad ? 'bg-[#b42318]' : 'bg-[#1f2937]',
          )}
        >
          {current.message}
        </div>
      )}
    </ToastContext.Provider>
  )
}
