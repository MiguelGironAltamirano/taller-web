/** Descarga un texto como archivo desde el navegador (p. ej. exportación CSV). */
export function downloadText(filename: string, content: string, mime = 'text/csv;charset=utf-8') {
  // El BOM permite que Excel abra el CSV en UTF-8 con tildes correctas.
  const blob = new Blob(['﻿', content], { type: mime })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  document.body.appendChild(link)
  link.click()
  link.remove()
  URL.revokeObjectURL(url)
}
