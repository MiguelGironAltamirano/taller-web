import type { UserSummary } from '../api/types'

export type Session = { accessToken: string; user: UserSummary }

type Listener = () => void

let current: Session | null = null
const listeners = new Set<Listener>()

/** In-memory session holder. Never persisted to web storage. */
export const sessionStore = {
  get(): Session | null {
    return current
  },
  set(session: Session | null) {
    current = session
    listeners.forEach((listener) => listener())
  },
  clear() {
    this.set(null)
  },
  subscribe(listener: Listener): () => void {
    listeners.add(listener)
    return () => listeners.delete(listener)
  },
}
