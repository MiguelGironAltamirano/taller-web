import { describe, expect, it } from 'vitest'
import { areaSchema, categorySchema, locationSchema, prioritySchema, userSchema } from './forms'

describe('userSchema', () => {
  const ok = { fullName: 'Nuevo Técnico', email: 'ntecnico@unmsm.edu.pe', role: 'TECNICO', areaId: '1' }
  it('acepta técnicos y supervisores con área', () => {
    expect(userSchema.safeParse(ok).success).toBe(true)
  })
  it('exige área para técnico y supervisor, pero no para solicitante ni administrador', () => {
    expect(userSchema.safeParse({ ...ok, areaId: '' }).success).toBe(false)
    expect(userSchema.safeParse({ ...ok, role: 'SUPERVISOR', areaId: '' }).success).toBe(false)
    expect(userSchema.safeParse({ ...ok, role: 'SOLICITANTE', areaId: '' }).success).toBe(true)
    expect(userSchema.safeParse({ ...ok, role: 'ADMINISTRADOR', areaId: '' }).success).toBe(true)
  })
  it('solo admite correos @unmsm.edu.pe', () => {
    const r = userSchema.safeParse({ ...ok, email: 'x@gmail.com' })
    expect(r.success).toBe(false)
    if (!r.success) expect(r.error.issues[0].message).toBe('Usa un correo @unmsm.edu.pe.')
  })
  it('exige un nombre de 3 a 150 caracteres, como el backend', () => {
    expect(userSchema.safeParse({ ...ok, fullName: '  ' }).success).toBe(false)
    expect(userSchema.safeParse({ ...ok, fullName: 'Al' }).success).toBe(false)
    expect(userSchema.safeParse({ ...ok, fullName: 'Ana' }).success).toBe(true)
    expect(userSchema.safeParse({ ...ok, fullName: 'a'.repeat(151) }).success).toBe(false)
  })
})

describe('catálogos', () => {
  it('área: exige nombre', () => {
    expect(areaSchema.safeParse({ name: '' }).success).toBe(false)
    expect(areaSchema.safeParse({ name: 'Biblioteca' }).success).toBe(true)
  })
  it('categoría: exige nombre y área', () => {
    expect(categorySchema.safeParse({ name: 'Pintura', areaId: '' }).success).toBe(false)
    expect(categorySchema.safeParse({ name: 'Pintura', areaId: '1' }).success).toBe(true)
  })
  it('prioridad: horas enteras mayores a 0', () => {
    expect(prioritySchema.safeParse({ slaHours: '0' }).success).toBe(false)
    expect(prioritySchema.safeParse({ slaHours: '-5' }).success).toBe(false)
    expect(prioritySchema.safeParse({ slaHours: '1.5' }).success).toBe(false)
    expect(prioritySchema.safeParse({ slaHours: 'abc' }).success).toBe(false)
    expect(prioritySchema.safeParse({ slaHours: '48' }).success).toBe(true)
    expect(prioritySchema.safeParse({ slaHours: '8760' }).success).toBe(true)
    expect(prioritySchema.safeParse({ slaHours: '8761' }).success).toBe(false)
  })
  it('ubicación: exige facultad, pabellón y ambiente', () => {
    expect(locationSchema.safeParse({ faculty: 'F', pavilion: 'P', room: '' }).success).toBe(false)
    expect(locationSchema.safeParse({ faculty: 'F', pavilion: 'P', room: 'A1' }).success).toBe(true)
  })
})
