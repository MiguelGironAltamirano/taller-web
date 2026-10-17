import { useState } from 'react'
import { ApiError } from '../../shared/api/client'
import type { EvidenceItem, RequestDetailView } from '../../shared/domain'
import { MAX_FILE_BYTES, MAX_FILES } from '../../shared/lib/files'
import { sessionStore } from '../../shared/session/session-store'
import { Icon, useToast } from '../../shared/ui'
import { useEvidences, useUploadEvidence } from './api'

const ALLOWED_REGEX = /\.(jpe?g|png|pdf)$/i

/** Pestaña "Evidencias": archivos adjuntos con su descarga y subida autenticada. */
export function EvidenceTab({ request }: { request: RequestDetailView }) {
  const toast = useToast()
  const { data: remoteEvidences } = useEvidences(request.code)
  const upload = useUploadEvidence(request.code)
  const [error, setError] = useState<string | null>(null)

  const evidences: EvidenceItem[] = remoteEvidences
    ? remoteEvidences.map((e) => ({
        id: e.id,
        name: e.fileName,
        kind: e.kind,
        sizeKb: Math.round(e.sizeBytes / 1024),
      }))
    : request.evidences

  const handleDownload = async (evidence: EvidenceItem) => {
    try {
      const session = sessionStore.get()
      const headers: Record<string, string> = {}
      if (session) headers.Authorization = `Bearer ${session.accessToken}`
      const response = await fetch(`/api/v1/requests/${request.code}/evidences/${evidence.id}`, { headers })
      if (!response.ok) throw new Error('Error al descargar')
      const blob = await response.blob()
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = evidence.name
      document.body.appendChild(a)
      a.click()
      a.remove()
      URL.revokeObjectURL(url)
    } catch {
      toast('No pudimos descargar la evidencia.', { bad: true })
    }
  }

  const handleFileChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    setError(null)
    const file = e.target.files?.[0]
    if (!file) return

    if (!ALLOWED_REGEX.test(file.name)) {
      setError(`"${file.name}": solo se permiten JPG, PNG o PDF.`)
      return
    }

    if (file.size > MAX_FILE_BYTES) {
      setError(`"${file.name}" supera los 5 MB.`)
      return
    }

    if (evidences.length >= MAX_FILES) {
      setError('Se ha alcanzado el límite de 5 evidencias para esta solicitud.')
      return
    }

    try {
      await upload.mutateAsync(file)
      toast('Evidencia subida correctamente.')
    } catch (err) {
      setError(err instanceof ApiError ? err.detail || err.title : 'No se pudo subir la evidencia.')
    } finally {
      e.target.value = ''
    }
  }

  return (
    <div className="flex flex-col gap-4">
      {evidences.length === 0 ? (
        <div className="py-2 text-muted">Esta solicitud no tiene evidencias.</div>
      ) : (
        <ul className="m-0 grid list-none grid-cols-[repeat(auto-fill,minmax(140px,1fr))] gap-2.5 p-0">
          {evidences.map((f) => (
            <li key={f.id} className="overflow-hidden rounded-[10px] border border-line">
              <div
                className="grid h-[90px] place-items-center font-mono text-[11px] text-muted"
                style={{ background: 'repeating-linear-gradient(135deg, var(--surface2) 0 8px, var(--surface) 8px 16px)' }}
              >
                {f.kind}
              </div>
              <div className="flex items-center gap-1.5 p-2">
                <span className="flex-1 truncate text-xs" title={f.name}>
                  {f.name}
                </span>
                <button
                  type="button"
                  aria-label={`Descargar ${f.name}`}
                  onClick={() => handleDownload(f)}
                  className="grid size-7 place-items-center rounded-md border-0 bg-transparent text-muted hover:text-foreground"
                >
                  <Icon name="download" size={14} />
                </button>
              </div>
            </li>
          ))}
        </ul>
      )}

      {evidences.length < MAX_FILES && (
        <div className="flex flex-col gap-2 border-t border-line pt-3">
          <label className="flex cursor-pointer items-center gap-2 text-xs font-medium text-primary">
            <Icon name="upload" size={16} />
            <span>Adjuntar evidencia (máx. 5 MB · JPG, PNG o PDF)</span>
            <input
              type="file"
              accept=".jpg,.jpeg,.png,.pdf"
              aria-label="Adjuntar archivo de evidencia"
              className="sr-only"
              disabled={upload.isPending}
              onChange={handleFileChange}
            />
          </label>
          {upload.isPending && <div className="text-xs text-muted">Subiendo evidencia...</div>}
          {error && <div role="alert" className="text-xs text-err-fg">{error}</div>}
        </div>
      )}
    </div>
  )
}
