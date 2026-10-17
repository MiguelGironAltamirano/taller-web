import { useCallback, useSyncExternalStore } from 'react'

const listeners = new Set<() => void>()

function read(key: string): boolean {
  try {
    return localStorage.getItem(key) === '1'
  } catch {
    return false
  }
}

/**
 * Bandera booleana persistida en localStorage (tema oscuro, menú lateral colapsado).
 * Solo es una preferencia de interfaz: nunca guarda datos sensibles.
 */
export function usePersistedFlag(key: string, onChange?: (value: boolean) => void): [boolean, (v: boolean) => void] {
  const value = useSyncExternalStore(
    (cb) => {
      listeners.add(cb)
      return () => listeners.delete(cb)
    },
    () => read(key),
    () => false,
  )
  const set = useCallback(
    (next: boolean) => {
      try {
        localStorage.setItem(key, next ? '1' : '0')
      } catch {
        /* almacenamiento no disponible: la preferencia solo dura la sesión */
      }
      onChange?.(next)
      listeners.forEach((l) => l())
    },
    [key, onChange],
  )
  return [value, set]
}
