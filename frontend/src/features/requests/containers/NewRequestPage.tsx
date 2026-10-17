import { zodResolver } from '@hookform/resolvers/zod'
import { useMemo, useState } from 'react'
import { useForm, useWatch } from 'react-hook-form'
import { useNavigate } from 'react-router'
import { buildLocationTree, useCatalogs } from '../../../shared/api/catalogs'
import { ApiError } from '../../../shared/api/client'
import type { Priority } from '../../../shared/domain'
import { useNow } from '../../../shared/hooks/useNow'
import { cn } from '../../../shared/lib/cn'
import { dateLong } from '../../../shared/lib/format'
import type { PickedFile } from '../../../shared/lib/files'
import {
  Button,
  Card,
  CardTitle,
  Field,
  FileDropzone,
  Icon,
  PageHeader,
  PriorityBadge,
  Select,
  StatusBadge,
  Textarea,
  useToast,
} from '../../../shared/ui'
import { SuggestionBanner, useTriageSuggestion } from '../../triage'
import { useCreateRequest } from '../api'
import { DESCRIPTION_MAX, newRequestSchema, type NewRequestValues } from '../schemas/newRequest'

const EMPTY: Partial<NewRequestValues> = { category: '', faculty: '', pavilion: '', room: '', description: '', priority: undefined }

// Campos del contrato de la API → campos del formulario.
const FIELD_MAP: Record<string, keyof NewRequestValues> = {
  categoryId: 'category',
  locationId: 'room',
  priorityId: 'priority',
  description: 'description',
}

/** Formulario "Nueva solicitud" (solicitante): validación en vivo, sugerencia de triage, evidencias y resumen lateral. */
export function NewRequestPage() {
  const navigate = useNavigate()
  const toast = useToast()
  const { data: catalogs } = useCatalogs()
  const create = useCreateRequest()
  const now = useNow()
  const [files, setFiles] = useState<PickedFile[]>([])
  const [ignored, setIgnored] = useState(false)
  const [created, setCreated] = useState<string | null>(null)

  const {
    register,
    handleSubmit,
    control,
    setValue,
    reset,
    setError,
    formState: { errors, isSubmitting, isSubmitted },
  } = useForm<NewRequestValues>({
    resolver: zodResolver(newRequestSchema),
    defaultValues: EMPTY as NewRequestValues,
    reValidateMode: 'onChange',
  })

  const [category, faculty, pavilion, room, description, priority] = useWatch({
    control,
    name: ['category', 'faculty', 'pavilion', 'room', 'description', 'priority'],
  })
  const tree = useMemo(() => buildLocationTree(catalogs?.locations ?? []), [catalogs])
  const pavilions = tree.find((f) => f.name === faculty)?.pavilions ?? []
  const rooms = pavilions.find((p) => p.name === pavilion)?.rooms ?? []
  const categoryInfo = catalogs?.categories.find((c) => c.name === category)
  const area = catalogs?.areas.find((a) => a.id === categoryInfo?.areaId)?.name
  const sla = catalogs?.priorities.find((p) => p.name === priority)?.slaHours

  const suggestion = useTriageSuggestion(description ?? '')
  const showSuggestion = suggestion && !ignored && (suggestion.category !== category || suggestion.priority !== priority)

  const onSubmit = handleSubmit(
    async (v) => {
      const body = {
        categoryId: catalogs!.categories.find((c) => c.name === v.category)!.id,
        locationId: rooms.find((r) => r.name === v.room)!.id,
        description: v.description.trim(),
        priorityId: catalogs!.priorities.find((p) => p.name === v.priority)!.id,
        evidences: files.map((f) => ({ fileName: f.name, sizeBytes: f.size })),
      }
      try {
        const res = await create.mutateAsync(body)
        setCreated(res.code)
        toast(`Solicitud ${res.code} registrada`)
      } catch (e) {
        if (e instanceof ApiError && e.fieldErrors) {
          for (const [field, message] of Object.entries(e.fieldErrors)) {
            const mapped = FIELD_MAP[field]
            if (mapped) setError(mapped, { message })
          }
        }
        toast('Revisa los campos marcados', { bad: true })
      }
    },
    () => toast('Revisa los campos marcados', { bad: true }),
  )

  if (created) {
    return (
      <Card className="mx-auto my-6 flex w-full max-w-[560px] flex-col items-center gap-2.5 p-6 text-center">
        <div className="grid size-12 place-items-center rounded-full bg-ok-bg text-ok-fg">
          <Icon name="check" size={24} strokeWidth={2} />
        </div>
        <h1 className="m-0 text-xl font-semibold">Solicitud registrada</h1>
        <div className="text-2xl font-bold tracking-wide">{created}</div>
        <StatusBadge state="Registrada" />
        <div className="text-muted">El área responsable la evaluará y te avisaremos de cada cambio.</div>
        <div className="mt-1.5 flex flex-wrap justify-center gap-2">
          <Button
            onClick={() => {
              reset(EMPTY as NewRequestValues)
              setFiles([])
              setIgnored(false)
              setCreated(null)
            }}
          >
            Crear otra
          </Button>
          <Button variant="primary" onClick={() => navigate(`/solicitudes/${created}`)}>
            Ver solicitud
          </Button>
        </div>
      </Card>
    )
  }

  const catErr = errors.category?.message
  return (
    <>
      <PageHeader title="Nueva solicitud" subtitle="Los campos con * son obligatorios." />
      <form onSubmit={onSubmit} noValidate className="flex flex-wrap items-start gap-3.5">
        <div className="flex min-w-0 flex-[2_1_560px] flex-col gap-3.5">
          <Card className="flex flex-col gap-3 p-4">
            <CardTitle className="mb-0">Datos de la solicitud</CardTitle>
            <Field
              label="Categoría *"
              error={catErr}
              hint={area && (
                <>
                  Área responsable: <b className="text-fg">{area}</b>
                </>
              )}
            >
              {(a) => (
                <Select {...a} invalid={!!catErr} className="h-[38px]" {...register('category')}>
                  <option value="">Selecciona una categoría</option>
                  {catalogs?.categories.map((c) => (
                    <option key={c.id} value={c.name}>
                      {c.name}
                    </option>
                  ))}
                </Select>
              )}
            </Field>
            <div className="flex flex-col gap-1.5 text-[13px] font-medium">
              <label htmlFor="description">Descripción *</label>
              <Textarea
                id="description"
                rows={5}
                invalid={!!errors.description}
                aria-describedby="description-help"
                placeholder="Describe el problema y desde cuándo ocurre"
                {...register('description', { onChange: () => setIgnored(false) })}
              />
              <div id="description-help" className="flex justify-between font-normal">
                <span className="text-err-fg">{errors.description?.message}</span>
                <span className="text-muted">
                  {(description ?? '').length} / {DESCRIPTION_MAX}
                </span>
              </div>
              {showSuggestion && (
                <SuggestionBanner
                  suggestion={suggestion}
                  onAccept={() => {
                    setValue('category', suggestion.category, { shouldValidate: isSubmitted })
                    setValue('priority', suggestion.priority, { shouldValidate: isSubmitted })
                  }}
                  onIgnore={() => setIgnored(true)}
                />
              )}
            </div>
            <fieldset className="m-0 flex flex-col gap-1.5 border-0 p-0">
              <legend className="mb-1.5 p-0 text-[13px] font-medium">Prioridad sugerida *</legend>
              <div className="grid grid-cols-[repeat(auto-fit,minmax(120px,1fr))] gap-2">
                {catalogs?.priorities.map((p) => (
                  <button
                    key={p.id}
                    type="button"
                    role="radio"
                    aria-checked={priority === p.name}
                    onClick={() => setValue('priority', p.name as Priority, { shouldValidate: true })}
                    className={cn(
                      'rounded-[10px] border-2 bg-surface px-3 py-2.5 text-left text-fg',
                      priority === p.name ? 'border-primary' : 'border-line',
                    )}
                  >
                    <PriorityBadge priority={p.name as Priority} />
                    <div className="mt-1.5 text-xs text-muted">Atención en {p.slaHours} h</div>
                  </button>
                ))}
              </div>
              {errors.priority && <span className="text-[13px] text-err-fg">{errors.priority.message}</span>}
            </fieldset>
          </Card>

          <Card className="p-4">
            <CardTitle>Ubicación</CardTitle>
            <div className="grid grid-cols-[repeat(auto-fit,minmax(180px,1fr))] gap-3">
              <Field label="Facultad *" error={errors.faculty?.message}>
                {(a) => (
                  <Select
                    {...a}
                    invalid={!!errors.faculty}
                    className="h-[38px]"
                    {...register('faculty', {
                      onChange: () => {
                        setValue('pavilion', '')
                        setValue('room', '')
                      },
                    })}
                  >
                    <option value="">Selecciona</option>
                    {tree.map((f) => (
                      <option key={f.id} value={f.name}>
                        {f.name}
                      </option>
                    ))}
                  </Select>
                )}
              </Field>
              <Field label="Pabellón *" error={errors.pavilion?.message}>
                {(a) => (
                  <Select
                    {...a}
                    disabled={!faculty}
                    invalid={!!errors.pavilion}
                    className="h-[38px]"
                    {...register('pavilion', { onChange: () => setValue('room', '') })}
                  >
                    <option value="">Selecciona</option>
                    {pavilions.map((p) => (
                      <option key={p.id} value={p.name}>
                        {p.name}
                      </option>
                    ))}
                  </Select>
                )}
              </Field>
              <Field label="Ambiente *" error={errors.room?.message}>
                {(a) => (
                  <Select {...a} disabled={!pavilion} invalid={!!errors.room} className="h-[38px]" {...register('room')}>
                    <option value="">Selecciona</option>
                    {rooms.map((r) => (
                      <option key={r.id} value={r.name}>
                        {r.name}
                      </option>
                    ))}
                  </Select>
                )}
              </Field>
            </div>
          </Card>

          <Card className="flex flex-col gap-2.5 p-4">
            <CardTitle className="mb-0">
              Evidencias <span className="text-[13px] font-normal text-muted">(opcional)</span>
            </CardTitle>
            <FileDropzone files={files} onChange={setFiles} />
          </Card>
        </div>

        <Card className="sticky top-0 flex min-w-0 flex-[1_1_280px] flex-col gap-2.5 p-4" aria-label="Resumen">
          <CardTitle className="mb-0">Resumen</CardTitle>
          <dl className="m-0 grid grid-cols-[auto_1fr] gap-x-3 gap-y-2 text-[13px]">
            <dt className="text-muted">Categoría</dt>
            <dd className="m-0">{category || '—'}</dd>
            <dt className="text-muted">Área</dt>
            <dd className="m-0">{area ?? '—'}</dd>
            <dt className="text-muted">Ubicación</dt>
            <dd className="m-0">{room ? `${room}, ${pavilion}` : '—'}</dd>
            <dt className="text-muted">Prioridad</dt>
            <dd className="m-0">{priority || '—'}</dd>
            <dt className="text-muted">Evidencias</dt>
            <dd className="m-0">{files.length ? `${files.length} adjunto(s)` : 'Ninguna'}</dd>
          </dl>
          <div className="rounded-[10px] bg-surface2 p-3">
            <div className="text-xs text-muted">Tiempo estimado de atención</div>
            <div className="text-xl font-bold">{sla ? `${sla} horas` : '—'}</div>
            <div className="text-xs text-muted">
              {sla ? `Vencería el ${dateLong(now + sla * 3_600_000)}` : 'Elige una prioridad'}
            </div>
          </div>
          <Button type="submit" variant="primary" size="lg" loading={isSubmitting || create.isPending}>
            {isSubmitting || create.isPending ? 'Registrando…' : 'Registrar solicitud'}
          </Button>
          <Button onClick={() => navigate('/solicitudes')}>Cancelar</Button>
        </Card>
      </form>
    </>
  )
}
