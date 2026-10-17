export const MAX_FILES = 5
export const MAX_FILE_BYTES = 5 * 1024 * 1024
const ALLOWED = /\.(jpe?g|png|pdf)$/i

export type PickedFile = { name: string; size: number; isPdf: boolean; previewUrl: string | null }

/**
 * Valida y agrega archivos respetando las reglas de evidencias (spec §5.1):
 * hasta 5 archivos, 5 MB cada uno, solo JPG, PNG o PDF.
 */
export function addFiles(current: PickedFile[], incoming: File[]): { files: PickedFile[]; error: string } {
  const files = [...current]
  let error = ''
  for (const file of incoming) {
    if (!ALLOWED.test(file.name)) {
      error = `"${file.name}": solo se permiten JPG, PNG o PDF.`
      continue
    }
    if (file.size > MAX_FILE_BYTES) {
      error = `"${file.name}" supera los 5 MB.`
      continue
    }
    if (files.length >= MAX_FILES) {
      error = 'Máximo 5 archivos.'
      break
    }
    const isPdf = /\.pdf$/i.test(file.name)
    files.push({ name: file.name, size: file.size, isPdf, previewUrl: isPdf ? null : URL.createObjectURL(file) })
  }
  return { files, error }
}
