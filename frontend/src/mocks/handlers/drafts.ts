import { http, HttpResponse } from 'msw'
import type { RequestDraft, RequestDraftFields } from '../../shared/api/types'
import { getDb } from '../db'
import { iso, problem, secured } from '../http'

const FIELDS = ['category', 'description', 'priority', 'faculty', 'pavilion', 'room'] as const
const MAX: Record<(typeof FIELDS)[number], number> = {
  category: 150,
  description: 2000,
  priority: 150,
  faculty: 150,
  pavilion: 150,
  room: 150,
}

/** Borrador de «Nueva solicitud», uno por solicitante (contrato «Borrador de nueva solicitud»). */
export const draftHandlers = [
  http.get(
    '*/api/v1/me/request-draft',
    secured(['SOLICITANTE'], ({ user }) => {
      const draft = getDb().drafts[user.id]
      return draft ? HttpResponse.json(draft) : new HttpResponse(null, { status: 204 })
    }),
  ),

  http.put(
    '*/api/v1/me/request-draft',
    secured(['SOLICITANTE'], async ({ request, user }) => {
      const body = (await request.json()) as Partial<RequestDraftFields>
      const errors: Record<string, string> = {}
      const fields = Object.fromEntries(FIELDS.map((f) => [f, body[f] ?? ''])) as RequestDraftFields
      for (const f of FIELDS) if (fields[f].length > MAX[f]) errors[f] = `Puede tener hasta ${MAX[f]} caracteres`
      if (Object.keys(errors).length) return problem(400, 'Bad Request', 'La solicitud contiene datos inválidos', errors)
      const db = getDb()
      if (FIELDS.every((f) => !fields[f].trim())) {
        delete db.drafts[user.id]
        return new HttpResponse(null, { status: 204 })
      }
      const draft: RequestDraft = { ...fields, updatedAt: iso(Date.now()) }
      db.drafts[user.id] = draft
      return HttpResponse.json(draft)
    }),
  ),

  http.delete(
    '*/api/v1/me/request-draft',
    secured(['SOLICITANTE'], ({ user }) => {
      delete getDb().drafts[user.id]
      return new HttpResponse(null, { status: 204 })
    }),
  ),
]
