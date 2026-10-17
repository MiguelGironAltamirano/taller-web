import { cn } from '../../lib/cn'

type StarRatingProps = { value: number; onChange: (value: number) => void; label?: string }

/** Calificación de 1 a 5 estrellas (radiogroup accesible). */
export function StarRating({ value, onChange, label = 'Calificación' }: StarRatingProps) {
  return (
    <div role="radiogroup" aria-label={label} className="flex gap-1">
      {[1, 2, 3, 4, 5].map((n) => (
        <button
          key={n}
          type="button"
          role="radio"
          aria-checked={value === n}
          aria-label={`${n} ${n === 1 ? 'estrella' : 'estrellas'}`}
          onClick={() => onChange(n)}
          className={cn('size-9 border-0 bg-transparent max-md:size-11', n <= value ? 'text-[#d97706]' : 'text-muted')}
        >
          <svg
            width="26"
            height="26"
            viewBox="0 0 24 24"
            fill={n <= value ? 'currentColor' : 'none'}
            stroke="currentColor"
            strokeWidth="1.5"
            strokeLinejoin="round"
            className="mx-auto"
            aria-hidden="true"
          >
            <path d="M12 2l3.1 6.3 6.9 1-5 4.9 1.2 6.8L12 17.8 5.8 21l1.2-6.8-5-4.9 6.9-1z" />
          </svg>
        </button>
      ))}
    </div>
  )
}
