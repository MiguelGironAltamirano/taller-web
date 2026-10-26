import { useEffect, useState } from 'react'

/** Devuelve `value` cuando deja de cambiar durante `delayMs` (p. ej. para no consultar en cada tecla). */
export function useDebouncedValue<T>(value: T, delayMs = 300): T {
  const [debounced, setDebounced] = useState(value)
  useEffect(() => {
    const id = setTimeout(() => setDebounced(value), delayMs)
    return () => clearTimeout(id)
  }, [value, delayMs])
  return debounced
}
