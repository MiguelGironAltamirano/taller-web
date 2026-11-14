import '@testing-library/jest-dom/vitest'
import { cleanup, configure } from '@testing-library/react'
import { afterAll, afterEach, beforeAll } from 'vitest'
import { sessionStore } from '../shared/session/session-store'
import { server } from './msw-server'

// Las pantallas se cargan con rutas `lazy`: la primera importación en jsdom puede pasar del segundo por defecto.
configure({ asyncUtilTimeout: 5000 })

// jsdom no implementa estas APIs del navegador que usa la interfaz (layout responsive, scroll, gráficos).
window.matchMedia ??= ((query: string) => ({
  matches: false,
  media: query,
  onchange: null,
  addEventListener: () => {},
  removeEventListener: () => {},
  addListener: () => {},
  removeListener: () => {},
  dispatchEvent: () => false,
})) as typeof window.matchMedia
Element.prototype.scrollTo ??= () => {}
globalThis.ResizeObserver ??= class {
  observe() {}
  unobserve() {}
  disconnect() {}
}

beforeAll(() => server.listen({ onUnhandledFrame: 'error' }))
afterEach(() => {
  server.resetHandlers()
  cleanup()
  sessionStore.clear()
  sessionStorage.clear() // la "cookie" de renovación simulada por MSW
})
afterAll(() => server.close())
