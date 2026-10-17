import { setupWorker } from 'msw/browser'
import { handlers } from './handlers'

/** Arranca la API simulada en el navegador (solo con `VITE_ENABLE_MOCKS=true`). */
export async function startMocks() {
  const worker = setupWorker(...handlers)
  await worker.start({ onUnhandledFrame: 'bypass', quiet: true })
}
