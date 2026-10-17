import { useSyncExternalStore } from 'react'

function subscribe(query: string, onChange: () => void) {
  const mql = window.matchMedia(query)
  mql.addEventListener('change', onChange)
  return () => mql.removeEventListener('change', onChange)
}

export function useMediaQuery(query: string): boolean {
  return useSyncExternalStore(
    (cb) => subscribe(query, cb),
    () => window.matchMedia(query).matches,
    () => false,
  )
}

/** Móvil = menos de 768 px (breakpoint `md` de Tailwind). */
export function useIsMobile(): boolean {
  return useMediaQuery('(max-width: 767.98px)')
}
