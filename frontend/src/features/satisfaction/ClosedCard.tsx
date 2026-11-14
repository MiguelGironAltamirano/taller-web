import { useState } from 'react'
import { ApiError } from '../../shared/api/client'
import type { RequestDetailView } from '../../shared/domain'
import { Button, StarRating, Textarea, useToast } from '../../shared/ui'
import { RATING_COMMENT_MAX, useRateRequest } from './api'

type Props = { request: RequestDetailView; isOwner: boolean }

const cardClass = 'flex flex-col gap-2.5 rounded-xl border border-line bg-surface p-4'

/**
 * Satisfacción de una solicitud cerrada: la calificación (para el solicitante y el personal que ve la solicitud), la
 * opción de calificar después de confirmar si aún no lo hizo y el aviso del cierre automático a las 72 h.
 */
export function ClosedCard({ request, isOwner }: Props) {
  if (request.closureReason === 'AUTOMATICA') {
    return (
      <section aria-labelledby="closed-title" className={cardClass}>
        <h2 id="closed-title" className="m-0 text-[15px] font-semibold">
          Cerrada automáticamente
        </h2>
        <p className="m-0 text-muted">
          {isOwner
            ? 'La solicitud se cerró porque no confirmaste ni rechazaste la solución en 72 horas.'
            : 'El solicitante no respondió en 72 horas, así que la solicitud se cerró sin calificación.'}
        </p>
      </section>
    )
  }
  if (request.closureReason !== 'CONFIRMADA') return null
  if (request.rating) {
    return (
      <section aria-labelledby="closed-title" className={cardClass}>
        <h2 id="closed-title" className="m-0 text-[15px] font-semibold">
          {isOwner ? 'Tu calificación' : 'Calificación del solicitante'}
        </h2>
        <StarRating readOnly value={request.rating.score} label={isOwner ? 'Tu calificación' : 'Calificación'} />
        {request.rating.comment && <p className="m-0 leading-relaxed">“{request.rating.comment}”</p>}
      </section>
    )
  }
  return isOwner ? <RateForm code={request.code} /> : null
}

function RateForm({ code }: { code: string }) {
  const toast = useToast()
  const rate = useRateRequest(code)
  const [score, setScore] = useState(0)
  const [comment, setComment] = useState('')

  const submit = async (e: React.FormEvent) => {
    e.preventDefault()
    try {
      await rate.mutateAsync({ score, comment: comment.trim() || undefined })
      toast('Gracias por tu calificación.')
    } catch (err) {
      toast(err instanceof ApiError ? err.detail || err.title : 'No pudimos guardar tu calificación.', { bad: true })
    }
  }

  return (
    <form aria-labelledby="rate-title" onSubmit={submit} className={cardClass}>
      <h2 id="rate-title" className="m-0 text-[15px] font-semibold">
        ¿Cómo te atendimos?
      </h2>
      <p className="m-0 text-muted">Confirmaste la solución. Calificar es opcional y nos ayuda a mejorar.</p>
      <StarRating value={score} onChange={setScore} />
      <Textarea
        rows={2}
        aria-label="Comentario"
        placeholder="Comentario (opcional)"
        maxLength={RATING_COMMENT_MAX}
        value={comment}
        onChange={(e) => setComment(e.target.value)}
      />
      <div>
        <Button type="submit" variant="primary" disabled={score === 0} loading={rate.isPending}>
          Enviar calificación
        </Button>
      </div>
    </form>
  )
}
