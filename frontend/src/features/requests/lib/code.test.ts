import { describe, expect, it } from 'vitest'
import { formatRequestCode, isRequestCode } from './code'

describe('formatRequestCode', () => {
  it('genera SOL-AAAA-NNNNNN con correlativo de 6 dígitos', () => {
    expect(formatRequestCode(2026, 1)).toBe('SOL-2026-000001')
    expect(formatRequestCode(2026, 123)).toBe('SOL-2026-000123')
  })
})

describe('isRequestCode', () => {
  it('valida el formato', () => {
    expect(isRequestCode('SOL-2026-000123')).toBe(true)
    expect(isRequestCode('SOL-26-123')).toBe(false)
    expect(isRequestCode('sol-2026-000123')).toBe(false)
  })
})
