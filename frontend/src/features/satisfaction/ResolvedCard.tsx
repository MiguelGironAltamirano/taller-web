import { useState } from 'react'
import { ApiError } from '../../shared/api/client'
import type { RequestDetailView } from '../../shared/domain'
import { Alert, Button, Modal, ModalActions, StarRating, Textarea, useToast } from '../../shared/ui'
import { useRate, useRejectSolution } from './api'

const REJECT_MIN = 10

/**
 * Tarjeta "¿Quedó resuelto?" para el solicitante dueño de una solicitud Resuelta.
 * Confirmar exige calificar de 1 a 5 (como en el diseño); si no responde en 72 h se cierra sola, sin calificación.
 */
export function ResolvedCard({ request }: { request: RequestDetailView }) {
  const toast = useToast()
  const rate = useRate(request.code)
  const reject = useRejectSolution(request.code)
  const [score, setScore] = useState(0)
  const [comment, setComment] = useState('')
  const [rejecting, setRejecting] = useState(false)
  const [reason, setReason] = useState('')
  const [reasonError, setReasonError] = useState('')

  const confirm = async () => {
    if (!score) {
      toast('Califica la atención de 1 a 5 estrellas', { bad: true })
      return
    }
    try {
      await rate.mutateAsync({ score, comment: comment.trim() || undefined })
      toast('Solicitud cerrada. Gracias por tu calificación.')
    } catch (e) {
      toast(e instanceof ApiError ? e.detail || e.title : 'No pudimos cerrar la solicitud.', { bad: true })
    }
  }

  const submitReject = async (e: React.FormEvent) => {
    e.preventDefault()
    if (reason.trim().length < REJECT_MIN) {
      setReasonError(`Indica un motivo de al menos ${REJECT_MIN} caracteres.`)
      return
    }
    try {
      await reject.mutateAsync(reason.trim())
      setRejecting(false)
      toast('Solución rechazada. La solicitud volvió a En atención.')
    } catch (err) {
      setReasonError(err instanceof ApiError ? err.detail || err.title : 'No pudimos rechazar la solución.')
    }
  }

  return (
    <section className="flex flex-col gap-2.5 rounded-xl border border-ok-fg bg-surface p-4">
      <h2 className="m-0 text-[15px] font-semibold">¿Quedó resuelto?</h2>
      <div className="rounded-lg bg-ok-bg px-3 py-2.5 text-ok-fg">
        <b>Solución propuesta:</b> {request.solution}
      </div>
      <StarRating value={score} onChange={setScore} />
      <Textarea
        rows={2}
        aria-label="Comentario"
        placeholder="Comentario (opcional)"
        value={comment}
        onChange={(e) => setComment(e.target.value)}
      />
      <div className="flex flex-wrap gap-2">
        <Button variant="primary" size="lg" loading={rate.isPending} onClick={confirm}>
          Confirmar y cerrar
        </Button>
        <Button size="lg" onClick={() => setRejecting(true)}>
          Rechazar solución
        </Button>
      </div>
      <div className="text-xs text-muted">Si no respondes en 72 horas, la solicitud se cerrará automáticamente.</div>

      <Modal open={rejecting} onClose={() => setRejecting(false)} title="Rechazar solución" onSubmit={submitReject} width="min(440px,92%)">
        <div className="text-muted">La solicitud volverá a En atención. Indica qué falta resolver.</div>
        <Textarea
          rows={4}
          aria-label="Motivo"
          placeholder="Motivo del rechazo"
          invalid={!!reasonError}
          value={reason}
          onChange={(e) => {
            setReason(e.target.value)
            setReasonError('')
          }}
        />
        {reasonError && <Alert>{reasonError}</Alert>}
        <ModalActions>
          <Button onClick={() => setRejecting(false)}>Cancelar</Button>
          <Button type="submit" variant="danger" loading={reject.isPending}>
            Rechazar solución
          </Button>
        </ModalActions>
      </Modal>
    </section>
  )
}
