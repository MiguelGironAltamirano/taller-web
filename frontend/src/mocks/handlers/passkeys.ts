import { http, HttpResponse } from 'msw'
import type { PasskeyDto } from '../../shared/api/types'
import { getDb, newId, type MockPasskey } from '../db'
import { iso, problem, secured, tokenFor } from '../http'
import { summaryOfUser } from './auth'

/**
 * Passkeys simuladas (docs/api/passkeys-v1.md). Mismas rutas, cuerpos y errores que el backend, pero sin criptografía:
 * el mock confía en el id de la credencial que devuelve el navegador. Sirve para `pnpm dev:mocks` y las pruebas.
 */

const MAX_PASSKEYS = 10
const RP = { id: 'localhost', name: 'Servicios Universitarios UNMSM' }
const challenges = new Map<string, { purpose: 'LOGIN' | 'REGISTRATION'; userId: number | null }>()

let challengeSeq = 0

/** Bytes de relleno en base64url; el mock no necesita aleatoriedad criptográfica (ni `crypto`, que jsdom no trae). */
const randomBase64url = (bytes: number) =>
  btoa(String.fromCharCode(...Array.from({ length: bytes }, () => Math.floor(Math.random() * 256))))
    .replace(/\+/g, '-')
    .replace(/\//g, '_')
    .replace(/=+$/, '')

const issue = (purpose: 'LOGIN' | 'REGISTRATION', userId: number | null) => {
  const challengeId = `mock-challenge-${++challengeSeq}`
  challenges.set(challengeId, { purpose, userId })
  return { challengeId, challenge: randomBase64url(32) }
}

/** Un desafío sirve una sola vez y solo para su ceremonia (y su usuario, al registrar). */
const consume = (challengeId: string, purpose: 'LOGIN' | 'REGISTRATION', userId: number | null) => {
  const found = challenges.get(challengeId)
  challenges.delete(challengeId)
  return !!found && found.purpose === purpose && found.userId === userId
}

const toDto = (p: MockPasskey): PasskeyDto => ({
  id: p.id,
  name: p.name,
  createdAt: iso(p.createdAt),
  lastUsedAt: p.lastUsedAt === null ? null : iso(p.lastUsedAt),
  synced: false,
})

const loginFailed = () => problem(401, 'Unauthorized', 'No pudimos verificar tu passkey')

type CredentialBody = { challengeId: string; name?: string; credential: { id?: string } }

export const passkeyHandlers = [
  http.post('*/api/v1/auth/passkeys/login/options', () => {
    const { challengeId, challenge } = issue('LOGIN', null)
    return HttpResponse.json({
      challengeId,
      publicKey: { challenge, rpId: RP.id, timeout: 60000, userVerification: 'required', allowCredentials: [] },
    })
  }),

  http.post('*/api/v1/auth/passkeys/login', async ({ request }) => {
    const body = (await request.json()) as CredentialBody
    if (!consume(body.challengeId, 'LOGIN', null)) return loginFailed()
    const db = getDb()
    const passkey = db.passkeys.find((p) => p.credentialId === body.credential?.id)
    const user = passkey && db.users.find((u) => u.id === passkey.userId && u.active)
    if (!passkey || !user) return loginFailed()
    passkey.lastUsedAt = Date.now()
    try {
      sessionStorage.setItem('mock_refresh_user', String(user.id))
    } catch {
      /* sin almacenamiento: la sesión no sobrevive a recargas */
    }
    return HttpResponse.json({ accessToken: tokenFor(user), expiresIn: 900, user: summaryOfUser(user) })
  }),

  http.post(
    '*/api/v1/me/passkeys/registration/options',
    secured(null, async ({ request, user }) => {
      const body = (await request.json()) as { currentPassword?: string }
      if (!body.currentPassword) {
        return problem(400, 'Bad Request', 'La solicitud contiene datos inválidos', {
          currentPassword: 'Ingresa tu contraseña actual',
        })
      }
      if (body.currentPassword !== user.password) {
        return problem(400, 'Bad Request', 'La contraseña actual no es correcta', {
          currentPassword: 'La contraseña actual no es correcta.',
        })
      }
      const mine = getDb().passkeys.filter((p) => p.userId === user.id)
      if (mine.length >= MAX_PASSKEYS) {
        return problem(409, 'Conflict', `Ya tienes ${MAX_PASSKEYS} passkeys registradas. Elimina una para agregar otra`)
      }
      const { challengeId, challenge } = issue('REGISTRATION', user.id)
      return HttpResponse.json({
        challengeId,
        publicKey: {
          challenge,
          rp: RP,
          user: { id: randomBase64url(64), name: user.email, displayName: user.fullName },
          pubKeyCredParams: [
            { type: 'public-key', alg: -7 },
            { type: 'public-key', alg: -257 },
          ],
          timeout: 60000,
          excludeCredentials: mine.map((p) => ({ type: 'public-key', id: p.credentialId, transports: [] })),
          authenticatorSelection: { residentKey: 'required', requireResidentKey: true, userVerification: 'required' },
          attestation: 'none',
        },
      })
    }),
  ),

  http.post(
    '*/api/v1/me/passkeys',
    secured(null, async ({ request, user }) => {
      const body = (await request.json()) as CredentialBody
      if (!consume(body.challengeId, 'REGISTRATION', user.id) || !body.credential?.id) {
        return problem(400, 'Bad Request', 'No pudimos registrar la passkey. Vuelve a intentarlo')
      }
      const db = getDb()
      if (db.passkeys.some((p) => p.credentialId === body.credential.id)) {
        return problem(409, 'Conflict', 'Esta passkey ya está registrada')
      }
      const now = Date.now()
      const passkey: MockPasskey = {
        id: newId(),
        userId: user.id,
        credentialId: body.credential.id,
        name: body.name?.trim() || `Passkey del ${new Date(now).toLocaleDateString('es-PE', { timeZone: 'America/Lima' })}`,
        createdAt: now,
        lastUsedAt: null,
      }
      db.passkeys.push(passkey)
      return HttpResponse.json(toDto(passkey), { status: 201 })
    }),
  ),

  http.get(
    '*/api/v1/me/passkeys',
    secured(null, ({ user }) =>
      HttpResponse.json(
        getDb()
          .passkeys.filter((p) => p.userId === user.id)
          .sort((a, b) => b.createdAt - a.createdAt)
          .map(toDto),
      ),
    ),
  ),

  http.delete(
    '*/api/v1/me/passkeys/:id',
    secured(null, ({ params, user }) => {
      const db = getDb()
      const index = db.passkeys.findIndex((p) => p.id === Number(params.id) && p.userId === user.id)
      if (index < 0) return problem(404, 'Not Found', 'La passkey no existe')
      db.passkeys.splice(index, 1)
      return new HttpResponse(null, { status: 204 })
    }),
  ),
]
