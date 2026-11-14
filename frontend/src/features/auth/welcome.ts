const KEY = 'unmsm_welcome'

/** Marca que el usuario acaba de iniciar sesión (la consume el layout para saludarlo una sola vez). */
export function markWelcome() {
  try {
    sessionStorage.setItem(KEY, '1')
  } catch {
    /* almacenamiento no disponible: simplemente no se saluda */
  }
}

/** Devuelve si hay que saludar y borra la marca. */
export function consumeWelcome(): boolean {
  try {
    const pending = sessionStorage.getItem(KEY) === '1'
    sessionStorage.removeItem(KEY)
    return pending
  } catch {
    return false
  }
}
