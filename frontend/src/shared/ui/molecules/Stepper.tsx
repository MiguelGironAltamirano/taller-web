import { cn } from '../../lib/cn'

type StepperProps = { steps: readonly string[]; current: number; label?: string }

/** Indicador de progreso del flujo de estados. */
export function Stepper({ steps, current, label = 'Progreso' }: StepperProps) {
  return (
    <ol aria-label={label} className="m-0 flex min-w-[560px] list-none p-0 md:min-w-0">
      {steps.map((step, i) => {
        const done = i < current
        const active = i === current
        return (
          <li
            key={step}
            aria-current={active ? 'step' : undefined}
            className="relative flex flex-1 flex-col items-center gap-1.5 text-center"
          >
            {i > 0 && (
              <span
                aria-hidden="true"
                className={cn(
                  'absolute left-[-50%] top-[13px] h-0.5 w-full transition-colors',
                  i <= current ? 'bg-primary' : 'bg-line',
                )}
              />
            )}
            {/* z-10 evita que la línea del paso siguiente tape el círculo */}
            <span
              className={cn(
                'relative z-10 grid size-7 place-items-center rounded-full border-2 text-xs font-bold transition-colors',
                done && 'border-primary bg-primary text-on-primary',
                active && 'border-primary bg-primary/10 text-primary-fg ring-2 ring-primary/30 ring-offset-1',
                !done && !active && 'border-line bg-surface text-muted',
              )}
            >
              {done ? (
                <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                  <path d="M20 6 9 17l-5-5" />
                </svg>
              ) : (
                i + 1
              )}
            </span>
            <span
              className={cn(
                'text-xs transition-colors',
                active ? 'font-semibold text-fg' : done ? 'font-medium text-muted' : 'font-medium text-muted/70',
              )}
            >
              {step}
            </span>
          </li>
        )
      })}
    </ol>
  )
}
