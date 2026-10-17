import { zodResolver } from '@hookform/resolvers/zod'
import { useQueryClient } from '@tanstack/react-query'
import { useMemo } from 'react'
import { useForm, useWatch } from 'react-hook-form'
import { ApiError } from '../../shared/api/client'
import { useCatalogs } from '../../shared/api/catalogs'
import { requestsKey } from '../../shared/api/keys'
import type { Priority, WorkflowActionId } from '../../shared/domain'
import { dateLong } from '../../shared/lib/format'
import { Alert, Button, Field, Modal, ModalActions, Select, Textarea, useToast } from '../../shared/ui'
import { useTechnicians, useWorkflowAction } from './api'
import { ACTION_CONFIG } from './lib/transitions'
import { actionSchema, toApiCall, type ActionValues } from './schemas/action'

/** Lo mínimo que necesita una acción de flujo; lo cumplen tanto las filas como el detalle. */
export type ActionTarget = {
  code: string
  area: string
  priority: Priority
  technician: string | null
  createdAt: number
}

type Props = { action: WorkflowActionId; target: ActionTarget; onClose: () => void }

/** Diálogo de confirmación de una acción de flujo (asignar, reasignar, prioridad, cerrar, resolver…). */
export function ActionModal({ action, target, onClose }: Props) {
  const config = ACTION_CONFIG[action]
  const toast = useToast()
  const client = useQueryClient()
  const { data: catalogs } = useCatalogs()
  const areaId = catalogs?.areas.find((a) => a.name === target.area)?.id
  const technicians = useTechnicians(config.needTech ? areaId : undefined)
  const run = useWorkflowAction(target.code)

  const initialPriority = String(catalogs?.priorities.find((p) => p.name === target.priority)?.id ?? '')
  const schema = useMemo(() => actionSchema(config), [config])
  const {
    register,
    handleSubmit,
    control,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<ActionValues>({
    resolver: zodResolver(schema),
    values: { technicianId: '', priorityId: initialPriority, reason: '' },
  })

  const chosenId = useWatch({ control, name: 'priorityId' })
  const chosenPriority = catalogs?.priorities.find((p) => String(p.id) === chosenId)
  const newDue = chosenPriority ? dateLong(target.createdAt + chosenPriority.slaHours * 3_600_000) : ''
  const techOptions = (technicians.data ?? []).filter((t) => t.fullName !== target.technician)

  const onSubmit = handleSubmit(async (values) => {
    try {
      await run.mutateAsync(toApiCall(action, values))
      toast(`${config.title}: listo. Historial y notificación actualizados.`)
      onClose()
    } catch (e) {
      if (e instanceof ApiError && e.status === 409) {
        // Otro usuario cambió la solicitud antes: se avisa y se refresca para mostrar su estado actual.
        toast('Transición no permitida', { bad: true })
        await client.invalidateQueries({ queryKey: requestsKey })
        onClose()
        return
      }
      if (e instanceof ApiError && e.fieldErrors) {
        const f = e.fieldErrors
        if (f.technicianId) setError('technicianId', { message: f.technicianId })
        if (f.reason) setError('reason', { message: f.reason })
        if (f.priorityId) setError('priorityId', { message: f.priorityId })
      }
      setError('root', { message: e instanceof ApiError ? e.detail || e.title : 'No pudimos completar la acción.' })
    }
  })

  return (
    <Modal open onClose={onClose} title={config.title} subtitle={target.code} onSubmit={onSubmit}>
      {config.message && <div className="text-muted">{config.message}</div>}
      {config.needTech && (
        <Field label="Técnico del área" error={errors.technicianId?.message}>
          {(a) => (
            <Select {...a} invalid={!!errors.technicianId} {...register('technicianId')}>
              <option value="">Selecciona un técnico</option>
              {techOptions.map((t) => (
                <option key={t.id} value={t.id}>
                  {t.fullName}
                  {t.activeRequests !== undefined && ` · ${t.activeRequests} solicitudes activas`}
                </option>
              ))}
            </Select>
          )}
        </Field>
      )}
      {config.needPrio && (
        <Field label="Nueva prioridad" error={errors.priorityId?.message} hint={newDue && `Nuevo vencimiento: ${newDue}`}>
          {(a) => (
            <Select {...a} invalid={!!errors.priorityId} {...register('priorityId')}>
              {(catalogs?.priorities ?? []).map((p) => (
                <option key={p.id} value={p.id}>
                  {p.name} · {p.slaHours} h
                </option>
              ))}
            </Select>
          )}
        </Field>
      )}
      {config.needReason && (
        <Field label={config.reasonLabel ?? 'Motivo'} error={errors.reason?.message}>
          {(a) => <Textarea {...a} rows={3} invalid={!!errors.reason} {...register('reason')} />}
        </Field>
      )}
      {errors.root && <Alert>{errors.root.message}</Alert>}
      <ModalActions>
        <Button onClick={onClose}>Cancelar</Button>
        <Button type="submit" variant={config.danger ? 'danger' : 'primary'} loading={isSubmitting}>
          {config.cta}
        </Button>
      </ModalActions>
    </Modal>
  )
}
