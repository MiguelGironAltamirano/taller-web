import { useCallback, useEffect, useMemo, useRef, useState, type ReactNode } from 'react'
import { cn } from '../../lib/cn'
import { Icon, type IconName } from '../atoms/Icon'
import { ToastContext, type ToastApi, type ToastOptions } from './toast-context'

type Tone = 'success' | 'error' | 'info'
type ToastState = { message: string; tone: Tone; id: number }

const DURATION: Record<Tone, number> = { success: 4000, info: 5000, error: 7000 }

const TONE: Record<Tone, { icon: IconName; badge: string; bar: string; title: string }> = {
  success: { icon: 'check', badge: 'bg-ok-bg text-ok-fg', bar: 'bg-good', title: 'Listo' },
  error: { icon: 'alert', badge: 'bg-err-bg text-err-fg', bar: 'bg-err-fg', title: 'No se pudo completar' },
  info: { icon: 'bell', badge: 'bg-info-bg text-info-fg', bar: 'bg-info-fg', title: 'Aviso' },
}

function toastTone(opts?: ToastOptions): Tone {
  if (opts?.tone) return opts.tone
  return opts?.bad ? 'error' : 'success'
}

export function ToastProvider({ children }: { children: ReactNode }) {
  const [current, setCurrent] = useState<ToastState | null>(null)
  const [paused, setPaused] = useState(false)
  const timer = useRef<ReturnType<typeof setTimeout> | null>(null)
  const remaining = useRef(0)
  const startedAt = useRef(0)

  const clear = () => {
    if (timer.current) clearTimeout(timer.current)
    timer.current = null
  }

  const schedule = useCallback((ms: number) => {
    clear()
    remaining.current = ms
    startedAt.current = Date.now()
    timer.current = setTimeout(() => setCurrent(null), ms)
  }, [])

  useEffect(() => clear, [])

  const toast = useCallback<ToastApi['toast']>(
    (message, options) => {
      const tone = toastTone(options)
      setPaused(false)
      setCurrent({ message, tone, id: Date.now() })
      schedule(DURATION[tone])
    },
    [schedule],
  )

  const pause = () => {
    if (!timer.current) return
    clear()
    remaining.current -= Date.now() - startedAt.current
    setPaused(true)
  }
  const resume = () => {
    if (!paused) return
    setPaused(false)
    schedule(Math.max(remaining.current, 1200))
  }
  const dismiss = () => {
    clear()
    setCurrent(null)
  }

  const api = useMemo(() => ({ toast }), [toast])
  const tone = current ? TONE[current.tone] : null

  return (
    <ToastContext.Provider value={api}>
      {children}
      {current && tone && (
        <div
          role={current.tone === 'error' ? 'alert' : 'status'}
          key={current.id}
          onMouseEnter={pause}
          onMouseLeave={resume}
          onFocus={pause}
          onBlur={resume}
          className={cn(
            'fixed bottom-[76px] left-1/2 z-[90] w-[min(92vw,380px)] -translate-x-1/2 animate-toast-in overflow-hidden',
            'rounded-xl border border-line bg-surface text-fg shadow-[0_12px_32px_-8px_rgb(0_0_0/0.28),0_2px_6px_rgb(0_0_0/0.06)]',
            'md:bottom-6 md:left-auto md:right-6 md:translate-x-0',
          )}
        >
          <div className="flex items-start gap-3 py-3 pl-3.5 pr-2">
            <span className={cn('mt-0.5 grid size-8 shrink-0 place-items-center rounded-full', tone.badge)}>
              <Icon name={tone.icon} size={16} strokeWidth={2.25} />
            </span>
            <div className="min-w-0 flex-1 pt-px">
              <div className="text-[13px] font-semibold">{tone.title}</div>
              <div className="mt-0.5 text-[13px] leading-snug text-muted">{current.message}</div>
            </div>
            <button
              type="button"
              aria-label="Cerrar aviso"
              onClick={dismiss}
              className="grid size-7 shrink-0 place-items-center rounded-md border-0 bg-transparent text-muted hover:bg-surface2 hover:text-fg"
            >
              <Icon name="x" size={15} strokeWidth={2} />
            </button>
          </div>
          <div aria-hidden="true" className="h-[3px] bg-line/60">
            <div
              className={cn('h-full origin-left animate-toast-progress', tone.bar)}
              style={{
                animationDuration: `${DURATION[current.tone]}ms`,
                animationPlayState: paused ? 'paused' : 'running',
              }}
            />
          </div>
        </div>
      )}
    </ToastContext.Provider>
  )
}
