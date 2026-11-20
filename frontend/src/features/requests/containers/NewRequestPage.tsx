import { zodResolver } from '@hookform/resolvers/zod'
import { useEffect, useMemo, useState } from 'react'
import { useForm, useWatch } from 'react-hook-form'
import { useNavigate, useSearchParams } from 'react-router'
import { buildLocationTree, useCatalogs } from '../../../shared/api/catalogs'
import { ApiError } from '../../../shared/api/client'
import { QueryErrorState } from '../../../shared/api/QueryErrorState'
import type { RequestDraft, RequestDraftFields } from '../../../shared/api/types'
import type { Priority } from '../../../shared/domain'
import { useNow } from '../../../shared/hooks/useNow'
import { cn } from '../../../shared/lib/cn'
import { dateLong, timeShort } from '../../../shared/lib/format'
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
  Alert,
  useToast,
} from '../../../shared/ui'
import { uploadEvidences } from '../../collaboration'
import { SuggestionBanner, useTriageSuggestion } from '../../triage'
import { useCreateRequest, useDiscardRequestDraft, useRequestDraft } from '../api'
import { useAutosavedDraft } from '../lib/useAutosavedDraft'
import { DESCRIPTION_MAX, newRequestSchema, type NewRequestValues } from '../schemas/newRequest'

const EMPTY: Partial<NewRequestValues> = { category: '', faculty: '', pavilion: '', room: '', description: '', priority: undefined }

/** Los valores del borrador guardado, como valores del formulario (una prioridad vacía es «sin elegir»). */
const fromDraft = (d: RequestDraft): NewRequestValues =>
  ({
    category: d.category,
    description: d.description,
    priority: (d.priority || undefined) as Priority | undefined,
    faculty: d.faculty,
    pavilion: d.pavilion,
    room: d.room,
  }) as NewRequestValues

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
  const [params] = useSearchParams()
  const toast = useToast()
  const catalogsQuery = useCatalogs()
  const { data: catalogs } = catalogsQuery
  const create = useCreateRequest()
  const now = useNow()
  const [files, setFiles] = useState<PickedFile[]>([])
  const [ignored, setIgnored] = useState(false)
  const [created, setCreated] = useState<string | null>(null)
  /** Hubo cambios hechos por la persona: solo entonces se guarda (un formulario vacío no pisa un borrador). */
  const [interacted, setInteracted] = useState(false)
  /** El borrador guardado que se le ofrece retomar; `resumed` muestra el aviso de las evidencias. */
  const [offer, setOffer] = useState<'pending' | 'resolved' | 'resumed'>('pending')
  const draft = useRequestDraft()
  const discard = useDiscardRequestDraft()

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

  const draftValues: RequestDraftFields = {
    category: category ?? '',
    description: description ?? '',
    priority: priority ?? '',
    faculty: faculty ?? '',
    pavilion: pavilion ?? '',
    room: room ?? '',
  }
  // Escribir sin retomar el borrador ofrecido empieza una solicitud nueva, que lo reemplaza (así lo dice la oferta).
  const offerPending = offer === 'pending' && Boolean(draft.data) && !interacted
  const autosave = useAutosavedDraft(draftValues, interacted && !created)

  const resume = (saved: RequestDraft) => {
    reset(fromDraft(saved))
    autosave.markSaved({
      category: saved.category,
      description: saved.description,
      priority: saved.priority,
      faculty: saved.faculty,
      pavilion: saved.pavilion,
      room: saved.room,
    })
    setInteracted(true)
    setOffer('resumed')
  }

  // Desde el inicio («Retomar borrador») se llega con ?retomar=1: el borrador se aplica sin preguntar.
  const resumeNow = params.get('retomar') === '1'
  useEffect(() => {
    if (resumeNow && offer === 'pending' && draft.data) resume(draft.data)
    // eslint-disable-next-line react-hooks/exhaustive-deps -- solo cuando llega el borrador
  }, [resumeNow, draft.data])

  const discardDraft = () => {
    discard.mutate(undefined, {
      onError: () => toast('No pudimos descartar el borrador. Inténtalo de nuevo.', { tone: 'error' }),
    })
    setOffer('resolved')
  }

  /** Cancelar no pierde lo escrito: se guarda como borrador y se dice dónde retomarlo. */
  const cancel = async () => {
    if (interacted) {
      await autosave.flush()
      toast('Puedes retomarlo desde «Nueva solicitud» o desde el inicio.', {
        title: 'Guardamos tu borrador',
        action: { label: 'Descartar borrador', onClick: () => discard.mutate() },
      })
    }
    navigate('/solicitudes')
  }

  const suggestion = useTriageSuggestion(description ?? '')
  const showSuggestion = suggestion && !ignored && (suggestion.category !== category || suggestion.priority !== priority)

  const onSubmit = handleSubmit(
    async (v) => {
      const body = {
        categoryId: catalogs!.categories.find((c) => c.name === v.category)!.id,
        locationId: rooms.find((r) => r.name === v.room)!.id,
        description: v.description.trim(),
        priorityId: catalogs!.priorities.find((p) => p.name === v.priority)!.id,
      }
      let res: Awaited<ReturnType<typeof create.mutateAsync>>
      try {
        res = await create.mutateAsync(body)
      } catch (e) {
        if (e instanceof ApiError && e.fieldErrors) {
          for (const [field, message] of Object.entries(e.fieldErrors)) {
            const mapped = FIELD_MAP[field]
            if (mapped) setError(mapped, { message })
          }
        }
        toast('Revisa los campos marcados', { tone: 'error' })
        return
      }
      // The request exists from here on: evidences go to it afterwards and a failed upload never undoes it.
      const failed = await uploadEvidences(
        res.code,
        files.map((f) => f.file),
      )
      setCreated(res.code)
      if (failed) {
        toast(`Solicitud ${res.code} registrada, pero no pudimos adjuntar ${failed} evidencia(s). Súbelas desde el detalle.`, {
          tone: 'error',
        })
      } else {
        toast(`Solicitud ${res.code} registrada`)
      }
    },
    () => toast('Revisa los campos marcados', { tone: 'error' }),
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
              setInteracted(false)
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
      <PageHeader icon="plus" title="Nueva solicitud" subtitle="Los campos con * son obligatorios." />
      {catalogsQuery.isError && !catalogs && (
        <Card>
          <QueryErrorState what="las categorías, prioridades y ubicaciones" query={catalogsQuery} compact />
        </Card>
      )}
      {offerPending && draft.data && (
        <section
          aria-label="Borrador sin enviar"
          className="flex flex-wrap items-center gap-3 rounded-xl border border-line bg-info-bg px-4 py-3 text-info-fg"
        >
          <Icon name="file" size={18} className="shrink-0" />
          <p className="m-0 min-w-0 flex-1 text-[13px]">
            <b>Tienes un borrador sin enviar</b>, guardado el {dateLong(Date.parse(draft.data.updatedAt))}. Si empiezas
            una solicitud nueva, el borrador se reemplazará.
          </p>
          <div className="flex gap-2">
            <Button size="sm" onClick={discardDraft}>
              Descartar
            </Button>
            <Button size="sm" variant="primary" onClick={() => resume(draft.data!)}>
              Retomar
            </Button>
          </div>
        </section>
      )}
      {offer === 'resumed' && (
        <Alert tone="info">
          Retomaste tu borrador. Las evidencias no se guardan en el borrador: si tenías fotos o PDF, vuelve a adjuntarlos.
        </Alert>
      )}
      <form
        onSubmit={onSubmit}
        onChangeCapture={() => setInteracted(true)}
        noValidate
        className="flex flex-wrap items-start gap-3.5"
      >
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
                    // Ctrl+Z del navegador no deshace este cambio (lo hace la app, no la persona): se ofrece «Deshacer».
                    const previous = { category: category ?? '', priority }
                    setValue('category', suggestion.category, { shouldValidate: isSubmitted })
                    setValue('priority', suggestion.priority, { shouldValidate: isSubmitted })
                    setInteracted(true)
                    toast(`Categoría ${suggestion.category} y prioridad ${suggestion.priority}.`, {
                      title: 'Sugerencia aplicada',
                      action: {
                        label: 'Deshacer',
                        onClick: () => {
                          setValue('category', previous.category, { shouldValidate: isSubmitted })
                          setValue('priority', previous.priority as Priority, { shouldValidate: isSubmitted })
                          setIgnored(true)
                        },
                      },
                    })
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
                    onClick={() => {
                      setValue('priority', p.name as Priority, { shouldValidate: true })
                      setInteracted(true)
                    }}
                    className={cn(
                      'rounded-[10px] border-2 bg-surface px-3 py-2.5 text-left text-fg',
                      priority === p.name ? 'border-primary' : 'border-line hover:border-line-strong hover:bg-surface2',
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
          <Button onClick={() => void cancel()}>Cancelar</Button>
          <p aria-live="polite" className="m-0 min-h-4 text-center text-xs text-muted">
            {autosave.status === 'saving' && 'Guardando borrador…'}
            {autosave.status === 'saved' && autosave.savedAt && `Borrador guardado · ${timeShort(autosave.savedAt)}`}
            {autosave.status === 'error' && (
              <span className="text-err-fg">No pudimos guardar el borrador. Lo intentaremos de nuevo al seguir escribiendo.</span>
            )}
          </p>
        </Card>
      </form>
    </>
  )
}
