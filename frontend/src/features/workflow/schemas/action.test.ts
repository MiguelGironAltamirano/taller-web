import { describe, expect, it } from 'vitest'
import { ACTION_CONFIG } from '../lib/transitions'
import { actionSchema, toApiCall } from './action'

describe('actionSchema', () => {
  it('asignar exige técnico', () => {
    const schema = actionSchema(ACTION_CONFIG.assign)
    expect(schema.safeParse({ technicianId: '', priorityId: '', reason: '' }).success).toBe(false)
    expect(schema.safeParse({ technicianId: '4', priorityId: '', reason: '' }).success).toBe(true)
  })

  it('reasignar exige técnico y motivo de al menos 5 caracteres', () => {
    const schema = actionSchema(ACTION_CONFIG.reassign)
    expect(schema.safeParse({ technicianId: '4', priorityId: '', reason: 'corto' }).success).toBe(true)
    expect(schema.safeParse({ technicianId: '4', priorityId: '', reason: 'cort' }).success).toBe(false)
    expect(schema.safeParse({ technicianId: '', priorityId: '', reason: 'motivo válido' }).success).toBe(false)
  })

  it('cerrar sin resolver y resolver exigen motivo', () => {
    for (const a of ['noproc', 'dup', 'resolve'] as const) {
      const schema = actionSchema(ACTION_CONFIG[a])
      expect(schema.safeParse({ technicianId: '', priorityId: '', reason: '   ' }).success, a).toBe(false)
      expect(schema.safeParse({ technicianId: '', priorityId: '', reason: 'Motivo claro' }).success, a).toBe(true)
    }
  })

  it('pasar a evaluación e iniciar atención no piden datos', () => {
    for (const a of ['eval', 'start'] as const) {
      expect(actionSchema(ACTION_CONFIG[a]).safeParse({ technicianId: '', priorityId: '', reason: '' }).success).toBe(true)
    }
  })
})

describe('toApiCall', () => {
  const empty = { technicianId: '', priorityId: '', reason: '' }

  it('traduce cada acción al contrato de transiciones', () => {
    expect(toApiCall('eval', empty)).toEqual({ kind: 'transition', body: { to: 'EN_EVALUACION' } })
    expect(toApiCall('assign', { ...empty, technicianId: '4' })).toEqual({
      kind: 'transition',
      body: { to: 'ASIGNADA', technicianId: 4 },
    })
    expect(toApiCall('reassign', { ...empty, technicianId: '5', reason: ' falta de carga ' })).toEqual({
      kind: 'transition',
      body: { to: 'ASIGNADA', technicianId: 5, reason: 'falta de carga' },
    })
    expect(toApiCall('start', empty)).toEqual({ kind: 'transition', body: { to: 'EN_ATENCION' } })
    expect(toApiCall('resolve', { ...empty, reason: 'Listo' })).toEqual({
      kind: 'transition',
      body: { to: 'RESUELTA', reason: 'Listo' },
    })
  })

  it('cerrar distingue No procede de Duplicada', () => {
    expect(toApiCall('noproc', { ...empty, reason: 'Fuera de alcance' })).toEqual({
      kind: 'transition',
      body: { to: 'CERRADA', closureReason: 'NO_PROCEDE', reason: 'Fuera de alcance' },
    })
    expect(toApiCall('dup', { ...empty, reason: 'Igual a SOL-1' })).toEqual({
      kind: 'transition',
      body: { to: 'CERRADA', closureReason: 'DUPLICADA', reason: 'Igual a SOL-1' },
    })
  })

  it('cambiar prioridad usa el endpoint de prioridad', () => {
    expect(toApiCall('prio', { ...empty, priorityId: '1' })).toEqual({ kind: 'priority', body: { priorityId: 1 } })
  })
})
