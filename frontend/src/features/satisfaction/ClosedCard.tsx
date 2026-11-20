import { useId, useState } from 'react'
import { errorMessage } from '../../shared/api/errors'
import type { RequestDetailView } from '../../shared/domain'
import { Button, StarRating, Textarea, useToast } from '../../shared/ui'
import { RATING_COMMENT_MAX } from '../../shared/api/types'
import { useRateRequest } from './api'

type Props = { request: RequestDetailView; isOwner: boolean }

const cardClass = 'flex flex-col gap-2.5 rounded-xl border border-line bg-surface p-4'

function SolutionBox({ solution }: { solution: string }) {
  return (
    <div className="rounded-lg bg-ok-bg px-3 py-2.5 text-ok-fg">
      <b>Solución aplicada:</b> {solution}
    </div>
  )
}

/**
 * Satisfacción de una solicitud cerrada: la calificación (para el solicitante y el personal que ve la solicitud), la
 * opción de calificar después de confirmar si aún no lo hizo y el aviso del cierre automático (el plazo lo configura
 * el administrador, así que el texto no fija un número de horas).
 */
export function ClosedCard({ request, isOwner }: Props) {
  const titleId = useId()
  if (request.closureReason === 'AUTOMATICA') {
    return (
      <section aria-labelledby={titleId} className={cardClass}>
        <h2 id={titleId} className="m-0 text-[15px] font-semibold">
          Cerrada automáticamente
        </h2>
        <p className="m-0 text-muted">
          {isOwner
            ? 'La solicitud se cerró porque no confirmaste ni rechazaste la solución dentro del plazo.'
            : 'El solicitante no respondió dentro del plazo, así que la solicitud se cerró sin calificación.'}
        </p>
      </section>
    )
  }
  if (request.closureReason !== 'CONFIRMADA') return null

  const solution = request.solution

  if (request.rating) {
    return (
      <section aria-labelledby={titleId} className={cardClass}>
        <h2 id={titleId} className="m-0 text-[15px] font-semibold">
          {isOwner ? 'Tu calificación' : 'Calificación del solicitante'}
        </h2>
        {solution && <SolutionBox solution={solution} />}
        <StarRating readOnly value={request.rating.score} label={isOwner ? 'Tu calificación' : 'Calificación'} />
        {request.rating.comment && <p className="m-0 leading-relaxed">{`“${request.rating.comment}”`}</p>}
      </section>
    )
  }
  if (isOwner) return <RateForm code={request.code} solution={solution} />
  return solution ? (
    <section aria-labelledby={titleId} className={cardClass}>
      <h2 id={titleId} className="m-0 text-[15px] font-semibold">
        Solución aplicada
      </h2>
      <div className="rounded-lg bg-ok-bg px-3 py-2.5 text-ok-fg">{solution}</div>
    </section>
  ) : null
}

function RateForm({ code, solution }: { code: string; solution: string | null }) {
  const toast = useToast()
  const rate = useRateRequest(code)
  const [score, setScore] = useState(0)
  const [comment, setComment] = useState('')
  const titleId = useId()

  const submit = async (e: React.FormEvent) => {
    e.preventDefault()
    try {
      await rate.mutateAsync({ score, comment: comment.trim() || undefined })
      toast('Gracias por tu calificación.')
    } catch (err) {
      toast(errorMessage(err, 'No pudimos guardar tu calificación.'), { tone: 'error' })
    }
  }

  return (
    <form aria-labelledby={titleId} onSubmit={submit} className={cardClass}>
      <h2 id={titleId} className="m-0 text-[15px] font-semibold">
        ¿Cómo te atendimos?
      </h2>
      {solution && <SolutionBox solution={solution} />}
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
