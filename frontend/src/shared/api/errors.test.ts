import { describe, expect, it } from 'vitest'
import { ApiError } from './client'
import { errorMessage } from './errors'

const FALLBACK = 'No pudimos guardar los cambios.'

describe('errorMessage', () => {
  it('uses the message of the server, ending it like a sentence', () => {
    expect(errorMessage(new ApiError(404, 'Not Found', 'La solicitud no existe'), FALLBACK)).toBe('La solicitud no existe.')
    expect(errorMessage(new ApiError(409, 'Conflict', 'Ya existe. Elige otro nombre.'), FALLBACK)).toBe(
      'Ya existe. Elige otro nombre.',
    )
  })

  it('never shows the technical title when the server sends no message', () => {
    expect(errorMessage(new ApiError(500, 'Internal Server Error'), FALLBACK)).toBe(
      'El servicio no está disponible en este momento. Inténtalo de nuevo en unos minutos.',
    )
    expect(errorMessage(new ApiError(502, 'Bad Gateway'), FALLBACK)).toMatch(/^El servicio no está disponible/)
  })

  it.each([
    [0, 'No pudimos conectar con el servidor. Revisa tu conexión e inténtalo de nuevo.'],
    [401, 'Tu sesión expiró. Vuelve a iniciar sesión.'],
    [403, 'No tienes permiso para realizar esta acción.'],
    [404, 'No encontramos lo que buscas. Puede que se haya eliminado.'],
    [409, 'Alguien más modificó estos datos. Actualiza la página e inténtalo de nuevo.'],
    [413, 'El archivo supera el tamaño máximo permitido de 5 MB.'],
    [429, 'Hiciste demasiados intentos. Espera un momento e inténtalo de nuevo.'],
  ])('explains a %i without a message from the server', (status, message) => {
    expect(errorMessage(new ApiError(status, 'Error'), FALLBACK)).toBe(message)
  })

  it('ignores an English message from something other than our API', () => {
    expect(errorMessage(new ApiError(403, 'Forbidden', 'You do not have permission to perform this action'), FALLBACK)).toBe(
      'No tienes permiso para realizar esta acción.',
    )
  })

  it('keeps a Spanish message that mentions an error', () => {
    expect(errorMessage(new ApiError(500, 'Error', 'Ocurrió un error inesperado. Inténtalo de nuevo en unos minutos'), FALLBACK)).toBe(
      'Ocurrió un error inesperado. Inténtalo de nuevo en unos minutos.',
    )
  })

  it('uses the fallback for a client error with no message of its own', () => {
    expect(errorMessage(new ApiError(400, 'Bad Request'), FALLBACK)).toBe(FALLBACK)
  })

  it('treats a failed connection as such and anything else as the fallback', () => {
    expect(errorMessage(new TypeError('Failed to fetch'), FALLBACK)).toMatch(/^No pudimos conectar/)
    expect(errorMessage(new Error('boom'), FALLBACK)).toBe(FALLBACK)
    expect(errorMessage(undefined, FALLBACK)).toBe(FALLBACK)
  })
})
