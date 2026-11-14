import { useCallback, useEffect, useMemo, useRef, useState, type ReactNode } from 'react'
import { cn } from '../../lib/cn'
import { ToastContext, type ToastApi, type ToastOptions } from './toast-context'

type Tone = 'success' | 'error' | 'info'
type ToastState = { message: string; tone: Tone; id: number }

const DURATION: Record<Tone, number> = { success: 3000, info: 3500, error: 5000 }

const TONE_CLASS: Record<Tone, string> = {
  success: 'bg-[#1f2937]',
  error: 'bg-[#b42318]',
  info: 'bg-[#1e40af]',
}

const TONE_ICON: Record<Tone, string> = {
  success: 'M20 6 9 17l-5-5',
  error: 'M18 6 6 18M6 6l12 12',
  info: 'M12 16v-4m0-4h.01 M12 21a9 9 0 1 0 0-18 9 9 0 0 0 0 18z',
}

function toastTone(opts?: ToastOptions): Tone {
  if (opts?.tone) return opts.tone
  return opts?.bad ? 'error' : 'success'
}

export function ToastProvider({ children }: { children: ReactNode }) {
  const [current, setCurrent] = useState<ToastState | null>(null)
  const timer = useRef<ReturnType<typeof setTimeout> | null>(null)

  useEffect(() => () => void (timer.current && clearTimeout(timer.current)), [])

  const toast = useCallback<ToastApi['toast']>((message, options) => {
    if (timer.current) clearTimeout(timer.current)
    const tone = toastTone(options)
    setCurrent({ message, tone, id: Date.now() })
    timer.current = setTimeout(() => setCurrent(null), DURATION[tone])
  }, [])

  const api = useMemo(() => ({ toast }), [toast])

  return (
    <ToastContext.Provider value={api}>
      {children}
      {current && (
        <div
          role={current.tone === 'error' ? 'alert' : 'status'}
          key={current.id}
          className={cn(
            'fixed bottom-[76px] left-1/2 z-[90] flex max-w-[92%] -translate-x-1/2 animate-pop items-center gap-2.5 rounded-[10px] px-4 py-2.5 text-[13px] font-medium text-white shadow-[0_8px_24px_rgb(0_0_0/0.25)] md:bottom-5',
            TONE_CLASS[current.tone],
          )}
        >
          <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true" className="shrink-0">
            <path d={TONE_ICON[current.tone]} />
          </svg>
          {current.message}
        </div>
      )}
    </ToastContext.Provider>
  )
}
