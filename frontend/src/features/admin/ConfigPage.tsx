import { zodResolver } from '@hookform/resolvers/zod'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { ApiError } from '../../shared/api/client'
import type { AppConfig } from '../../shared/api/types'
import { Alert, Button, Card, Field, Input, PageHeader, Skeleton, Switch, useToast } from '../../shared/ui'
import { useConfig, useSaveConfig } from './api'
import { configSchema } from './schemas/forms'

function ConfigForm({ config }: { config: AppConfig }) {
  const toast = useToast()
  const save = useSaveConfig()
  const [email, setEmail] = useState(config.emailNotifications)
  const [triage, setTriage] = useState(config.triageSuggestions)
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<{ autoCloseHours: string }>({
    resolver: zodResolver(configSchema),
    defaultValues: { autoCloseHours: String(config.autoCloseHours) },
  })

  const onSubmit = handleSubmit(async (v) => {
    try {
      await save.mutateAsync({ autoCloseHours: Number(v.autoCloseHours), emailNotifications: email, triageSuggestions: triage })
      toast('Configuración guardada')
    } catch (e) {
      setError('root', { message: e instanceof ApiError ? e.detail || e.title : 'No pudimos guardar la configuración.' })
    }
  })

  return (
    <Card className="flex max-w-[640px] flex-col gap-3.5 p-4">
      <form onSubmit={onSubmit} noValidate className="flex flex-col gap-3.5">
        <Field label="Cierre automático tras resolución (horas)" error={errors.autoCloseHours?.message}>
          {(a) => <Input {...a} inputMode="numeric" invalid={!!errors.autoCloseHours} className="max-w-40" {...register('autoCloseHours')} />}
        </Field>
        <div className="flex items-center gap-3">
          <Switch checked={email} onChange={setEmail} label="Notificaciones por correo" />
          <span>Enviar notificaciones por correo</span>
        </div>
        <div className="flex items-center gap-3">
          <Switch checked={triage} onChange={setTriage} label="Sugerencia automática" />
          <span>Sugerir categoría y prioridad al registrar</span>
        </div>
        {errors.root && <Alert>{errors.root.message}</Alert>}
        <div>
          <Button type="submit" variant="primary" size="lg" loading={isSubmitting}>
            Guardar cambios
          </Button>
        </div>
      </form>
    </Card>
  )
}

/** Parámetros generales: cierre automático, correo y sugerencias de triage. */
export function ConfigPage() {
  const { data } = useConfig()
  return (
    <>
      <PageHeader title="Configuración" subtitle="Parámetros generales del sistema" />
      {data ? <ConfigForm config={data} /> : <Skeleton className="h-64 max-w-[640px]" />}
    </>
  )
}
