import { useCallback, useEffect, useRef, useState } from 'react'
import type { RequestDraftFields } from '../../../shared/api/types'
import { useSaveRequestDraft } from '../api'

/** Espera tras la última tecla antes de guardar: suficiente para no guardar en cada letra. */
export const AUTOSAVE_DELAY_MS = 1200

export type AutosaveState = { status: 'idle' | 'saving' | 'saved' | 'error'; savedAt: number | null }

/**
 * Guarda el borrador de «Nueva solicitud» en el servidor mientras la persona escribe (contrato «Borrador de nueva
 * solicitud»): poco después de cada cambio, al ocultar la pestaña y al salir de la pantalla. Así, salga por donde
 * salga, lo escrito se puede retomar.
 *
 * `enabled` en `false` no guarda nada: antes de que la persona escriba (un formulario vacío no reemplaza un borrador
 * anterior), mientras decide si retoma uno, o tras registrar la solicitud.
 */
export function useAutosavedDraft(values: RequestDraftFields, enabled: boolean) {
  const save = useSaveRequestDraft()
  const [state, setState] = useState<AutosaveState>({ status: 'idle', savedAt: null })
  const serialized = JSON.stringify(values)
  const latest = useRef({ serialized, values, enabled })
  useEffect(() => {
    latest.current = { serialized, values, enabled }
  })
  const lastSaved = useRef<string | null>(null)
  const timer = useRef<ReturnType<typeof setTimeout> | null>(null)
  /** Al salir de la pantalla se guarda igual, pero ya no hay estado que mostrar: no se actualiza un componente ido. */
  const mounted = useRef(false)
  const report = useCallback((next: AutosaveState | ((s: AutosaveState) => AutosaveState)) => {
    if (mounted.current) setState(next)
  }, [])
  const { mutateAsync } = save

  const flush = useCallback(async () => {
    if (timer.current) clearTimeout(timer.current)
    timer.current = null
    const { serialized: now, values: current, enabled: on } = latest.current
    if (!on || now === lastSaved.current) return
    lastSaved.current = now
    report((s) => ({ ...s, status: 'saving' }))
    try {
      await mutateAsync(current)
      report({ status: 'saved', savedAt: Date.now() })
    } catch {
      lastSaved.current = null
      report((s) => ({ ...s, status: 'error' }))
    }
  }, [mutateAsync, report])

  useEffect(() => {
    if (!enabled || serialized === lastSaved.current) return
    if (timer.current) clearTimeout(timer.current)
    timer.current = setTimeout(() => void flush(), AUTOSAVE_DELAY_MS)
  }, [serialized, enabled, flush])

  // Cerrar o cambiar de pestaña, y salir de la pantalla por el menú, guardan lo pendiente.
  useEffect(() => {
    mounted.current = true
    const onHide = () => {
      if (document.visibilityState === 'hidden') void flush()
    }
    document.addEventListener('visibilitychange', onHide)
    return () => {
      document.removeEventListener('visibilitychange', onHide)
      mounted.current = false
      void flush()
    }
  }, [flush])

  /** Lo ya escrito se considera guardado (al retomar un borrador, no hay nada nuevo que enviar). */
  const markSaved = useCallback((fields: RequestDraftFields) => {
    lastSaved.current = JSON.stringify(fields)
  }, [])

  return { ...state, flush, markSaved }
}
