import type { UserSummary } from '../api/types'

export type Session = { accessToken: string; user: UserSummary }

/** Por qué terminó la sesión, cuando el usuario debe saberlo: `replaced` = ingresó en otro dispositivo. */
export type SessionEndReason = 'replaced'

type Listener = () => void

let current: Session | null = null
let endReason: SessionEndReason | null = null
const listeners = new Set<Listener>()

/** In-memory session holder. Never persisted to web storage. */
export const sessionStore = {
  get(): Session | null {
    return current
  },
  set(session: Session | null) {
    current = session
    if (session) endReason = null
    listeners.forEach((listener) => listener())
  },
  clear(reason: SessionEndReason | null = null) {
    endReason = reason
    this.set(null)
  },
  peekEndReason(): SessionEndReason | null {
    return endReason
  },
  /** Devuelve el motivo del último cierre una sola vez, para mostrarlo en el login. */
  takeEndReason(): SessionEndReason | null {
    const reason = endReason
    endReason = null
    return reason
  },
  subscribe(listener: Listener): () => void {
    listeners.add(listener)
    return () => listeners.delete(listener)
  },
}
