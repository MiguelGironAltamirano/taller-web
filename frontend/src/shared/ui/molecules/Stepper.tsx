import { cn } from '../../lib/cn'
import { Icon, type IconName } from '../atoms/Icon'

type StepperProps = {
  steps: readonly string[]
  current: number
  label?: string
  /** Ícono de cada etapa (mismo orden que `steps`). */
  icons?: readonly IconName[]
  /** Texto secundario bajo cada etapa, p. ej. la fecha en que se alcanzó. */
  captions?: readonly (string | undefined)[]
}

const STAGGER_MS = 90
// Radio del círculo (18 px) + separación con el conector (6 px).
const GAP = 24

/** Indicador de progreso del flujo de estados. */
export function Stepper({ steps, current, label = 'Progreso', icons, captions }: StepperProps) {
  return (
    <ol
      aria-label={label}
      className="m-0 grid min-w-[600px] list-none p-0 md:min-w-0"
      style={{ gridTemplateColumns: `repeat(${steps.length}, minmax(0, 1fr))` }}
    >
      {steps.map((step, i) => {
        const done = i < current
        const active = i === current
        const status = done ? 'completado' : active ? 'actual' : 'pendiente'
        const caption = captions?.[i] ?? (active ? 'Etapa actual' : undefined)
        return (
          <li
            key={step}
            aria-current={active ? 'step' : undefined}
            className="relative flex flex-col items-center px-1 text-center"
          >
            {i < steps.length - 1 && (
              <span
                aria-hidden="true"
                className="absolute top-[17px] h-[3px] overflow-hidden rounded-full bg-line"
                style={{ left: `calc(50% + ${GAP}px)`, right: `calc(-50% + ${GAP}px)` }}
              >
                {i < current && (
                  <span
                    className="block h-full origin-left animate-grow-x rounded-full bg-primary"
                    style={{ animationDelay: `${(i + 1) * STAGGER_MS}ms` }}
                  />
                )}
              </span>
            )}

            <span className="relative grid size-9 place-items-center">
              {active && <span aria-hidden="true" className="absolute inset-0 animate-halo rounded-full bg-primary" />}
              <span
                aria-hidden="true"
                className={cn(
                  'relative grid size-9 animate-step-in place-items-center rounded-full transition-colors duration-300',
                  done && 'bg-primary text-on-primary shadow-[0_1px_2px_rgb(0_0_0/0.18)]',
                  active &&
                    'border-2 border-primary bg-surface text-primary-fg shadow-[0_0_0_4px_var(--primary-soft),0_1px_3px_rgb(0_0_0/0.12)]',
                  !done && !active && 'border border-line-strong bg-surface text-muted',
                )}
                style={{ animationDelay: `${i * STAGGER_MS}ms` }}
              >
                {done ? (
                  <Icon name="check" size={16} strokeWidth={2.75} />
                ) : icons?.[i] ? (
                  <Icon name={icons[i]} size={16} strokeWidth={active ? 2 : 1.75} />
                ) : (
                  <span className="text-xs font-semibold tabular-nums">{i + 1}</span>
                )}
              </span>
            </span>

            <span
              className={cn(
                'mt-2 text-xs leading-tight',
                active ? 'font-semibold text-fg' : done ? 'font-medium text-fg' : 'font-medium text-muted',
              )}
            >
              {step}
              <span className="sr-only">, {status}</span>
            </span>
            {caption && (
              <span className={cn('mt-0.5 text-[11px] tabular-nums', active ? 'text-primary-fg' : 'text-muted')}>
                {caption}
              </span>
            )}
          </li>
        )
      })}
    </ol>
  )
}
