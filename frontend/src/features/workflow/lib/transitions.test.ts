import { describe, expect, it } from 'vitest'
import { actionsFromTransitions, availableActions, resolveTransition } from './transitions'

describe('availableActions', () => {
  it('el supervisor evalúa una solicitud registrada y puede cerrarla con motivo', () => {
    expect(availableActions('supervisor', 'Registrada')).toEqual(['eval', 'noproc', 'dup', 'prio'])
  })
  it('el supervisor asigna desde En evaluación', () => {
    expect(availableActions('supervisor', 'En evaluación')).toEqual(['assign', 'noproc', 'dup', 'prio'])
  })
  it('el supervisor solo reasigna o cambia prioridad estando Asignada', () => {
    expect(availableActions('supervisor', 'Asignada')).toEqual(['reassign', 'prio'])
  })
  it('el técnico inicia la atención y resuelve', () => {
    expect(availableActions('tecnico', 'Asignada')).toEqual(['start'])
    expect(availableActions('tecnico', 'En atención')).toEqual(['resolve'])
    expect(availableActions('tecnico', 'Registrada')).toEqual([])
  })
  it('el solicitante y el administrador no tienen acciones de flujo', () => {
    expect(availableActions('solicitante', 'Registrada')).toEqual([])
    expect(availableActions('administrador', 'En atención')).toEqual([])
  })
  it('nadie actúa sobre una solicitud Cerrada o Resuelta (salvo el solicitante)', () => {
    expect(availableActions('supervisor', 'Cerrada')).toEqual([])
    expect(availableActions('supervisor', 'Resuelta')).toEqual([])
    expect(availableActions('tecnico', 'Resuelta')).toEqual([])
  })
})

describe('resolveTransition', () => {
  it('traduce un movimiento de kanban a la acción permitida', () => {
    expect(resolveTransition('supervisor', 'Registrada', 'En evaluación')).toBe('eval')
    expect(resolveTransition('supervisor', 'En evaluación', 'Asignada')).toBe('assign')
    expect(resolveTransition('supervisor', 'Registrada', 'Cerrada')).toBe('noproc')
    expect(resolveTransition('tecnico', 'Asignada', 'En atención')).toBe('start')
    expect(resolveTransition('tecnico', 'En atención', 'Resuelta')).toBe('resolve')
  })
  it('rechaza transiciones no listadas', () => {
    expect(resolveTransition('supervisor', 'Registrada', 'Resuelta')).toBeNull()
    expect(resolveTransition('tecnico', 'Registrada', 'Asignada')).toBeNull()
    expect(resolveTransition('solicitante', 'Registrada', 'En evaluación')).toBeNull()
    expect(resolveTransition('supervisor', 'Resuelta', 'En atención')).toBeNull()
  })
})

describe('actionsFromTransitions', () => {
  it('traduce las transiciones que ofrece el servidor a las acciones de la interfaz', () => {
    expect(actionsFromTransitions('Registrada', ['En evaluación', 'Cerrada'])).toEqual(['eval', 'noproc', 'dup'])
    expect(actionsFromTransitions('En evaluación', ['Asignada', 'Cerrada'])).toEqual(['assign', 'noproc', 'dup'])
    expect(actionsFromTransitions('Asignada', ['Asignada'])).toEqual(['reassign'])
    expect(actionsFromTransitions('Asignada', ['En atención'])).toEqual(['start'])
    expect(actionsFromTransitions('En atención', ['Resuelta'])).toEqual(['resolve'])
  })

  it('no ofrece acciones sin transiciones permitidas', () => {
    expect(actionsFromTransitions('Registrada', [])).toEqual([])
    expect(actionsFromTransitions('Cerrada', [])).toEqual([])
  })

  it('confirmar o rechazar la solución no son acciones del flujo: las ofrece la tarjeta de satisfacción', () => {
    expect(actionsFromTransitions('Resuelta', ['Cerrada', 'En atención'])).toEqual([])
  })

  it('ignora destinos que no corresponden al estado actual', () => {
    expect(actionsFromTransitions('Registrada', ['Resuelta'])).toEqual([])
  })
})
