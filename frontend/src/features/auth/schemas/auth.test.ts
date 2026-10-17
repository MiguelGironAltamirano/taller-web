import { describe, expect, it } from 'vitest'
import { changePasswordSchema, loginSchema, registerSchema } from './auth'

describe('registerSchema', () => {
  const ok = {
    name: 'Lucía Quispe',
    email: 'lquispe@unmsm.edu.pe',
    type: 'Estudiante',
    password: 'Abcdef12',
    confirm: 'Abcdef12',
  }
  it('acepta un correo institucional', () => {
    expect(registerSchema.safeParse(ok).success).toBe(true)
  })
  it('rechaza correos fuera de unmsm.edu.pe', () => {
    const r = registerSchema.safeParse({ ...ok, email: 'lucia@gmail.com' })
    expect(r.success).toBe(false)
    if (!r.success) expect(r.error.issues[0].message).toBe('Usa tu correo institucional @unmsm.edu.pe.')
  })
  it('exige contraseña de al menos 8 caracteres y confirmación igual', () => {
    expect(registerSchema.safeParse({ ...ok, password: 'Ab1', confirm: 'Ab1' }).success).toBe(false)
    const r = registerSchema.safeParse({ ...ok, confirm: 'otra' })
    expect(r.success).toBe(false)
    if (!r.success) expect(r.error.issues[0].path).toEqual(['confirm'])
  })
})

describe('loginSchema', () => {
  it('exige correo y contraseña', () => {
    expect(loginSchema.safeParse({ email: '', password: '' }).success).toBe(false)
    expect(loginSchema.safeParse({ email: 'a@unmsm.edu.pe', password: 'x' }).success).toBe(true)
  })
})

describe('changePasswordSchema', () => {
  it('exige actual, nueva de 8+ y confirmación igual', () => {
    expect(changePasswordSchema.safeParse({ current: 'a', next: '12345678', confirm: '12345678' }).success).toBe(true)
    expect(changePasswordSchema.safeParse({ current: '', next: '12345678', confirm: '12345678' }).success).toBe(false)
    expect(changePasswordSchema.safeParse({ current: 'a', next: '1234', confirm: '1234' }).success).toBe(false)
    expect(changePasswordSchema.safeParse({ current: 'a', next: '12345678', confirm: '00000000' }).success).toBe(false)
  })
})
