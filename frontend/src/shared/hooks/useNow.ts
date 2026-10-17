import { useEffect, useState } from 'react'

/** Hora actual en milisegundos, refrescada cada minuto (para SLA y tiempos relativos). */
export function useNow(intervalMs = 60_000): number {
  const [now, setNow] = useState(() => Date.now())
  useEffect(() => {
    const id = setInterval(() => setNow(Date.now()), intervalMs)
    return () => clearInterval(id)
  }, [intervalMs])
  return now
}
