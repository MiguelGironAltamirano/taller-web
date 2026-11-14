import { cn } from '../../lib/cn'

type StarRatingProps =
  | { value: number; onChange: (value: number) => void; label?: string; readOnly?: false }
  | { value: number; readOnly: true; label?: string; onChange?: never }

function Star({ filled }: { filled: boolean }) {
  return (
    <svg
      width="26"
      height="26"
      viewBox="0 0 24 24"
      fill={filled ? 'currentColor' : 'none'}
      stroke="currentColor"
      strokeWidth="1.5"
      strokeLinejoin="round"
      className="mx-auto"
      aria-hidden="true"
    >
      <path d="M12 2l3.1 6.3 6.9 1-5 4.9 1.2 6.8L12 17.8 5.8 21l1.2-6.8-5-4.9 6.9-1z" />
    </svg>
  )
}

const starColor = (n: number, value: number) => (n <= value ? 'text-[#d97706]' : 'text-muted')

/**
 * Calificación de 1 a 5 estrellas. Editable: radiogroup accesible. Solo lectura: una imagen con su valor en texto
 * ("4 de 5 estrellas"), sin controles que el lector de pantalla anuncie como interactivos.
 */
export function StarRating(props: StarRatingProps) {
  const { value, label = 'Calificación' } = props
  if (props.readOnly) {
    return (
      <div role="img" aria-label={`${label}: ${value} de 5 estrellas`} className="flex gap-0.5">
        {[1, 2, 3, 4, 5].map((n) => (
          <span key={n} className={cn('size-6', starColor(n, value))}>
            <Star filled={n <= value} />
          </span>
        ))}
      </div>
    )
  }
  return (
    <div role="radiogroup" aria-label={label} className="flex gap-1">
      {[1, 2, 3, 4, 5].map((n) => (
        <button
          key={n}
          type="button"
          role="radio"
          aria-checked={value === n}
          aria-label={`${n} ${n === 1 ? 'estrella' : 'estrellas'}`}
          onClick={() => props.onChange(n)}
          className={cn('size-9 border-0 bg-transparent max-md:size-11', starColor(n, value))}
        >
          <Star filled={n <= value} />
        </button>
      ))}
    </div>
  )
}
