import { useState } from 'react'
import { ApiError } from '../../shared/api/client'
import type { RequestDetailView } from '../../shared/domain'
import { dateLong } from '../../shared/lib/format'
import { Avatar, Icon, Input, useToast } from '../../shared/ui'
import { useAddComment } from './api'

type Props = { request: RequestDetailView; currentUserName: string }

const MAX_LENGTH = 1000

/** Pestaña "Comentarios": conversación de la solicitud y campo para escribir. */
export function CommentsTab({ request, currentUserName }: Props) {
  const toast = useToast()
  const add = useAddComment(request.code)
  const [text, setText] = useState('')

  const submit = async (e: React.FormEvent) => {
    e.preventDefault()
    const value = text.trim()
    if (!value) return
    try {
      await add.mutateAsync({ text: value })
      setText('')
    } catch (err) {
      toast(err instanceof ApiError ? err.detail || err.title : 'No pudimos enviar el comentario.', { bad: true })
    }
  }

  return (
    <div className="flex flex-col gap-3">
      {request.comments.length === 0 && <div className="text-muted">Aún no hay comentarios.</div>}
      {request.comments.map((c) => (
        <div key={c.id} className="flex gap-2.5">
          <Avatar name={c.author} size="md" tone={c.author === currentUserName ? 'primary' : 'neutral'} />
          <div className="min-w-0 flex-1">
            <div className="text-[13px]">
              <b>{c.author}</b> <span className="text-muted">· {dateLong(c.at)}</span>
            </div>
            <div className="mt-1 rounded-[0_10px_10px_10px] bg-surface2 px-3 py-2 leading-normal">{c.text}</div>
          </div>
        </div>
      ))}
      <form onSubmit={submit} className="mt-1 flex gap-2">
        <Input
          value={text}
          maxLength={MAX_LENGTH}
          onChange={(e) => setText(e.target.value)}
          aria-label="Escribe un comentario"
          placeholder="Escribe un comentario"
          className="min-w-0 flex-1"
        />
        <button
          type="submit"
          aria-label="Enviar"
          disabled={add.isPending}
          className="grid h-10 w-11 place-items-center rounded-lg border-0 bg-primary text-on-primary disabled:opacity-60"
        >
          <Icon name="send" />
        </button>
      </form>
    </div>
  )
}
