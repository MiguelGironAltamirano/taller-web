import { useEffect } from 'react'
import { usePersistedFlag } from './usePersistedFlag'

const KEY = 'unmsm_dark'

/** Tema claro/oscuro: aplica la clase `dark` en <html> y recuerda la elección. */
export function useTheme(): { dark: boolean; toggle: () => void } {
  const [dark, setDark] = usePersistedFlag(KEY)
  useEffect(() => {
    document.documentElement.classList.toggle('dark', dark)
  }, [dark])
  return { dark, toggle: () => setDark(!dark) }
}
