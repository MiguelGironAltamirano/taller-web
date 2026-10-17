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
                className={cn('absolute left-[-50%] top-[13px] h-0.5 w-full', i <= current ? 'bg-primary' : 'bg-line')}
              />
            )}
            <span
              className={cn(
                'relative grid size-7 place-items-center rounded-full border-2 text-xs font-bold',
                done && 'border-primary bg-primary text-on-primary',
                active && 'border-primary bg-surface text-primary-fg',
                !done && !active && 'border-line bg-surface text-muted',
              )}
            >
              {done ? '✓' : i + 1}
            </span>
            <span className={cn('text-xs', active ? 'font-bold text-fg' : 'font-medium text-muted')}>{step}</span>
          </li>
        )
      })}
    </ol>
  )
}
