import { useId, useState } from 'react'
import { cn } from '../../lib/cn'
import { addFiles, type PickedFile } from '../../lib/files'
import { Icon } from '../atoms/Icon'

type FileDropzoneProps = {
  files: PickedFile[]
  onChange: (files: PickedFile[]) => void
}

export function FileDropzone({ files, onChange }: FileDropzoneProps) {
  const inputId = useId()
  const [dragging, setDragging] = useState(false)
  const [error, setError] = useState('')

  const take = (list: FileList | null) => {
    if (!list) return
    const result = addFiles(files, Array.from(list))
    setError(result.error)
    onChange(result.files)
  }

  return (
    <div className="flex flex-col gap-2.5">
      <label
        htmlFor={inputId}
        onDragOver={(e) => {
          e.preventDefault()
          setDragging(true)
        }}
        onDragLeave={() => setDragging(false)}
        onDrop={(e) => {
          e.preventDefault()
          setDragging(false)
          take(e.dataTransfer.files)
        }}
        className={cn(
          'flex cursor-pointer flex-col items-center gap-1.5 rounded-[10px] border-2 border-dashed p-5 text-center',
          dragging ? 'border-primary bg-primary-soft' : 'border-line bg-surface2',
        )}
      >
        <Icon name="upload" size={24} className="text-muted" />
        <span className="font-medium">
          Arrastra archivos o <u>selecciónalos</u>
        </span>
        <span className="text-xs text-muted">Hasta 5 archivos · máx. 5 MB cada uno · JPG, PNG o PDF</span>
        <input
          id={inputId}
          type="file"
          multiple
          accept=".jpg,.jpeg,.png,.pdf"
          className="sr-only"
          onChange={(e) => {
            take(e.target.files)
            e.target.value = ''
          }}
        />
      </label>
      {error && (
        <div role="alert" className="text-[13px] text-err-fg">
          {error}
        </div>
      )}
      {files.length > 0 && (
        <ul className="m-0 flex list-none flex-wrap gap-2.5 p-0">
          {files.map((f, i) => (
            <li key={`${f.name}-${i}`} className="flex max-w-60 items-center gap-2 rounded-lg border border-line py-1.5 pl-1.5 pr-2">
              <div
                className="grid size-10 shrink-0 place-items-center rounded-md bg-surface2 bg-cover bg-center text-[10px] font-bold text-muted"
                style={f.previewUrl ? { backgroundImage: `url(${f.previewUrl})` } : undefined}
              >
                {f.isPdf ? 'PDF' : ''}
              </div>
              <div className="min-w-0 text-xs">
                <div className="truncate">{f.name}</div>
                <div className="text-muted">{Math.round(f.size / 1024)} KB</div>
              </div>
              <button
                type="button"
                aria-label={`Quitar ${f.name}`}
                onClick={() => {
                  setError('')
                  onChange(files.filter((_, j) => j !== i))
                }}
                className="grid size-7 place-items-center rounded-md border-0 bg-transparent text-muted"
              >
                <Icon name="x" size={14} strokeWidth={2} />
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}
