import { http, HttpResponse } from 'msw'
import type {
  Catalogs,
  ChangePasswordBody,
  LoginBody,
  RegisterBody,
  Technician,
  TokenResponse,
  UserSummary,
} from '../../shared/api/types'
import { getDb, type MockUser } from '../db'
import { authenticate, problem, secured, tokenFor, unauthorized } from '../http'

const REFRESH_KEY = 'mock_refresh_user'

// Simula la cookie `refresh_token` (HttpOnly en el backend real) con sessionStorage.
const storeRefresh = (userId: number | null) => {
  try {
    if (userId === null) sessionStorage.removeItem(REFRESH_KEY)
    else sessionStorage.setItem(REFRESH_KEY, String(userId))
  } catch {
    /* sin almacenamiento: la sesión no sobrevive a recargas */
  }
}
const readRefresh = (): number | null => {
  try {
    const v = sessionStorage.getItem(REFRESH_KEY)
    return v ? Number(v) : null
  } catch {
    return null
  }
}

export const summaryOfUser = (u: MockUser): UserSummary => ({
  id: u.id,
  fullName: u.fullName,
  email: u.email,
  role: u.role,
  areaId: u.areaId,
})

const tokenResponse = (u: MockUser): TokenResponse => ({
  accessToken: tokenFor(u),
  expiresIn: 900,
  user: summaryOfUser(u),
})

const INSTITUTIONAL = /^[^\s@]+@unmsm\.edu\.pe$/i

export const authHandlers = [
  http.post('*/api/v1/auth/login', async ({ request }) => {
    const body = (await request.json()) as LoginBody
    const user = getDb().users.find((u) => u.email === body.email?.trim().toLowerCase())
    if (!user || user.password !== body.password || !user.active) {
      return problem(401, 'Credenciales inválidas', 'Correo o contraseña incorrectos.')
    }
    storeRefresh(user.id)
    return HttpResponse.json(tokenResponse(user))
  }),

  http.post('*/api/v1/auth/register', async ({ request }) => {
    const body = (await request.json()) as RegisterBody
    const errors: Record<string, string> = {}
    const name = body.fullName?.trim() ?? ''
    const email = body.email?.trim().toLowerCase() ?? ''
    const password = body.password ?? ''
    if (name.length < 3 || name.length > 150) errors.fullName = 'El nombre debe tener entre 3 y 150 caracteres.'
    if (!INSTITUTIONAL.test(email)) errors.email = 'Usa tu correo institucional @unmsm.edu.pe.'
    if (password.length < 8 || password.length > 72 || !/[A-Za-z]/.test(password) || !/\d/.test(password)) {
      errors.password = 'La contraseña debe tener de 8 a 72 caracteres, con al menos una letra y un número.'
    }
    if (!['ESTUDIANTE', 'DOCENTE', 'ADMINISTRATIVO'].includes(body.requesterType)) errors.requesterType = 'Tipo no válido.'
    if (Object.keys(errors).length) return problem(400, 'Datos no válidos', 'Revisa los campos marcados.', errors)

    const db = getDb()
    if (db.users.some((u) => u.email === email)) {
      return problem(409, 'Correo ya registrado', 'Ya existe una cuenta con ese correo.', { email: 'Ya existe una cuenta con ese correo.' })
    }
    const user: MockUser = {
      id: ++db.nextId,
      fullName: name,
      email,
      role: 'SOLICITANTE',
      areaId: null,
      requesterType: body.requesterType,
      active: true,
      password,
    }
    db.users.push(user)
    return HttpResponse.json(summaryOfUser(user), { status: 201 })
  }),

  http.post('*/api/v1/auth/refresh', () => {
    const id = readRefresh()
    const user = id ? getDb().users.find((u) => u.id === id && u.active) : undefined
    return user ? HttpResponse.json(tokenResponse(user)) : unauthorized()
  }),

  http.post('*/api/v1/auth/logout', () => {
    storeRefresh(null)
    return new HttpResponse(null, { status: 204 })
  }),

  http.get('*/api/v1/me', ({ request }) => {
    const user = authenticate(request)
    return user ? HttpResponse.json(summaryOfUser(user)) : unauthorized()
  }),

  http.put(
    '*/api/v1/auth/password',
    secured(null, async ({ request, user }) => {
      const body = (await request.json()) as ChangePasswordBody
      if (body.currentPassword !== user.password) {
        return problem(400, 'Datos no válidos', 'La contraseña actual no es correcta.', {
          currentPassword: 'La contraseña actual no es correcta.',
        })
      }
      if (!body.newPassword || body.newPassword.length < 8) {
        return problem(400, 'Datos no válidos', 'La nueva contraseña es muy corta.', {
          newPassword: 'La nueva contraseña debe tener al menos 8 caracteres.',
        })
      }
      user.password = body.newPassword
      return new HttpResponse(null, { status: 204 })
    }),
  ),
]

export const catalogHandlers = [
  http.get(
    '*/api/v1/catalogs',
    secured(null, () => {
      const { areas, categories, priorities, locations } = getDb()
      // Como el backend: solo áreas y categorías activas.
      const body: Catalogs = {
        areas: areas.filter((a) => a.active !== false).map(({ id, name }) => ({ id, name })),
        categories: categories.filter((c) => c.active !== false).map(({ id, name, areaId }) => ({ id, name, areaId })),
        priorities,
        locations,
      }
      return HttpResponse.json(body)
    }),
  ),

  http.get(
    '*/api/v1/users/technicians',
    secured(['SUPERVISOR', 'ADMINISTRADOR'], ({ request }) => {
      const db = getDb()
      const areaId = Number(new URL(request.url).searchParams.get('areaId'))
      const items: Technician[] = db.users
        .filter((u) => u.role === 'TECNICO' && u.active && (!areaId || u.areaId === areaId))
        .map((u) => ({
          id: u.id,
          fullName: u.fullName,
          activeRequests: db.requests.filter(
            (r) => r.technicianId === u.id && (r.status === 'ASIGNADA' || r.status === 'EN_ATENCION'),
          ).length,
        }))
      return HttpResponse.json(items)
    }),
  ),
]
