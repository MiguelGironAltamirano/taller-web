import { describe, expect, it } from 'vitest'
import { newRequestSchema } from './newRequest'

const valid = {
  category: 'Electricidad',
  faculty: 'Facultad de Medicina',
  pavilion: 'Pabellón Central',
  room: 'Aula 12',
  description: 'x'.repeat(20),
  priority: 'Alta',
}

describe('newRequestSchema', () => {
  it('acepta una solicitud con todos los datos obligatorios', () => {
    expect(newRequestSchema.safeParse(valid).success).toBe(true)
  })
  it('rechaza una descripción de 19 caracteres y acepta 20', () => {
    expect(newRequestSchema.safeParse({ ...valid, description: 'x'.repeat(19) }).success).toBe(false)
    expect(newRequestSchema.safeParse({ ...valid, description: 'x'.repeat(20) }).success).toBe(true)
  })
  it('rechaza una descripción de 2001 caracteres y acepta 2000', () => {
    expect(newRequestSchema.safeParse({ ...valid, description: 'x'.repeat(2000) }).success).toBe(true)
    expect(newRequestSchema.safeParse({ ...valid, description: 'x'.repeat(2001) }).success).toBe(false)
  })
  it('exige categoría, ubicación completa y prioridad', () => {
    for (const k of ['category', 'faculty', 'pavilion', 'room', 'priority'] as const) {
      expect(newRequestSchema.safeParse({ ...valid, [k]: '' }).success, k).toBe(false)
    }
  })
  it('no cuenta los espacios al inicio o final como texto escrito', () => {
    const padded = `${'x'.repeat(10)}${' '.repeat(15)}`
    expect(newRequestSchema.safeParse({ ...valid, description: padded }).success).toBe(false)
  })
})
