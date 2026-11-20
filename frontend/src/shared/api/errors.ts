import { ApiError } from './client'

/**
 * Mensaje para la persona a partir de cualquier error de una llamada (guía en docs/calidad/guia-mensajes.md).
 *
 * - Si el servidor explica el problema (`detail` de problem details, siempre en español), se usa ese texto.
 * - Si no (proxy caído, sin conexión, una respuesta sin cuerpo), se explica según el código, sin términos técnicos.
 * - En cualquier otro caso, `fallback`: lo que la pantalla intentaba hacer («No pudimos guardar la regla.»).
 *
 * Nunca devuelve el `title` técnico («Internal Server Error», «Bad Gateway»).
 */
export function errorMessage(error: unknown, fallback: string): string {
  if (error instanceof TypeError) return BY_STATUS[0]
  if (!(error instanceof ApiError)) return fallback
  if (error.detail && looksSpanish(error.detail)) return asSentence(error.detail)
  if (error.status >= 500) return UNAVAILABLE
  return BY_STATUS[error.status] ?? fallback
}

const UNAVAILABLE = 'El servicio no está disponible en este momento. Inténtalo de nuevo en unos minutos.'

const BY_STATUS: Record<number, string> = {
  0: 'No pudimos conectar con el servidor. Revisa tu conexión e inténtalo de nuevo.',
  401: 'Tu sesión expiró. Vuelve a iniciar sesión.',
  403: 'No tienes permiso para realizar esta acción.',
  404: 'No encontramos lo que buscas. Puede que se haya eliminado.',
  409: 'Alguien más modificó estos datos. Actualiza la página e inténtalo de nuevo.',
  413: 'El archivo supera el tamaño máximo permitido de 5 MB.',
  429: 'Hiciste demasiados intentos. Espera un momento e inténtalo de nuevo.',
}

/**
 * El backend responde en español, pero un proxy o una versión anterior podrían enviar texto del framework en inglés:
 * ese texto no se muestra. Basta con detectar las palabras de esos mensajes.
 */
function looksSpanish(text: string): boolean {
  return !/\b(the|is|not|required|failed|supported|permission|request|resource)\b/i.test(text)
}

function asSentence(text: string): string {
  const trimmed = text.trim()
  return /[.!?…]$/.test(trimmed) ? trimmed : `${trimmed}.`
}
