import { z } from 'zod'
import { INSTITUTIONAL_DOMAIN, REQUESTER_TYPES } from '../../../shared/domain'

const institutionalEmail = new RegExp(`^[^\\s@]+@${INSTITUTIONAL_DOMAIN.replace('.', '\\.')}$`, 'i')

export const PASSWORD_MIN = 8

export const loginSchema = z.object({
  email: z.string().trim().min(1, 'Ingresa tu correo.'),
  password: z.string().min(1, 'Ingresa tu contraseña.'),
})
export type LoginValues = z.infer<typeof loginSchema>

export const registerSchema = z
  .object({
    name: z.string().trim().min(1, 'Ingresa tu nombre completo.'),
    email: z
      .string()
      .trim()
      .min(1, 'Ingresa tu correo.')
      .refine((v) => v === '' || institutionalEmail.test(v), 'Usa tu correo institucional @unmsm.edu.pe.'),
    type: z.enum(REQUESTER_TYPES),
    password: z
      .string()
      .min(PASSWORD_MIN, `Mínimo ${PASSWORD_MIN} caracteres.`)
      .max(72, 'Máximo 72 caracteres.')
      .refine((v) => /[A-Za-z]/.test(v) && /\d/.test(v), 'Incluye al menos una letra y un número.'),
    confirm: z.string(),
  })
  .refine((v) => v.confirm !== '' && v.confirm === v.password, {
    message: 'Las contraseñas no coinciden.',
    path: ['confirm'],
  })
export type RegisterValues = z.infer<typeof registerSchema>

export const changePasswordSchema = z
  .object({
    current: z.string().min(1, 'Ingresa tu contraseña actual.'),
    next: z.string().min(PASSWORD_MIN, `La nueva contraseña debe tener al menos ${PASSWORD_MIN} caracteres.`),
    confirm: z.string(),
  })
  .refine((v) => v.next === v.confirm, { message: 'Las contraseñas no coinciden.', path: ['confirm'] })
export type ChangePasswordValues = z.infer<typeof changePasswordSchema>

/** 0 (vacía) a 4 (fuerte), para el indicador de fortaleza del registro. */
export function passwordScore(password: string): number {
  if (!password) return 0
  let score = 0
  if (password.length >= 8) score++
  if (/[A-Z]/.test(password) && /[a-z]/.test(password)) score++
  if (/\d/.test(password)) score++
  if (/[^A-Za-z0-9]/.test(password)) score++
  return Math.max(1, score)
}
