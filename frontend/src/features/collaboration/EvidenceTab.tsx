import type { RequestDetailView } from '../../shared/domain'
import { Icon, useToast } from '../../shared/ui'

/** Pestaña "Evidencias": archivos adjuntos con su descarga (solo para usuarios con acceso a la solicitud). */
export function EvidenceTab({ request }: { request: RequestDetailView }) {
  const toast = useToast()
  if (request.evidences.length === 0) return <div className="py-3 text-muted">Esta solicitud no tiene evidencias.</div>

  return (
    <ul className="m-0 grid list-none grid-cols-[repeat(auto-fill,minmax(140px,1fr))] gap-2.5 p-0">
      {request.evidences.map((f) => (
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
              onClick={() => toast('Descarga iniciada')}
              className="grid size-7 place-items-center rounded-md border-0 bg-transparent text-muted"
            >
              <Icon name="download" size={14} />
            </button>
          </div>
        </li>
      ))}
    </ul>
  )
}
