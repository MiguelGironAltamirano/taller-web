// Aplica el tema guardado antes de pintar, para evitar el parpadeo del modo claro al abrir en modo oscuro.
// Es un archivo propio (no un script en línea) porque la CSP de nginx solo permite scripts del mismo origen.
try {
  if (localStorage.getItem('unmsm_dark') === '1') document.documentElement.classList.add('dark')
} catch {
  // Sin acceso al almacenamiento: se queda el tema claro.
}
