import { zodResolver } from '@hookform/resolvers/zod'
import { useForm, type DefaultValues, type Path, type Resolver } from 'react-hook-form'
import type { ZodType } from 'zod'
import { ApiError } from '../../../shared/api/client'
import { Alert, Button, Field, Input, Modal, ModalActions, Select } from '../../../shared/ui'

export type FieldDef = {
  name: string
  label: string
  type: 'text' | 'select'
  options?: { value: string | number; label: string }[]
}

type Props<V extends Record<string, string>> = {
  title: string
  fields: FieldDef[]
  schema: ZodType<V>
  defaults: V
  cta: string
  onSubmit: (values: V) => Promise<unknown>
  onClose: () => void
}

/** Panel lateral genérico para crear o editar una entidad del panel de administración. */
export function EntityDrawer<V extends Record<string, string>>({ title, fields, schema, defaults, cta, onSubmit, onClose }: Props<V>) {
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<V>({
    resolver: zodResolver(schema as never) as Resolver<V>,
    defaultValues: defaults as DefaultValues<V>,
  })

  const submit = handleSubmit(async (values) => {
    try {
      await onSubmit(values)
      onClose()
    } catch (e) {
      if (e instanceof ApiError && e.fieldErrors) {
        for (const [field, message] of Object.entries(e.fieldErrors)) setError(field as Path<V>, { message })
      }
      setError('root', { message: e instanceof ApiError ? e.detail || e.title : 'No pudimos guardar los cambios.' })
    }
  })

  return (
    <Modal open onClose={onClose} variant="drawer" title={title} onSubmit={submit}>
      {fields.map((f) => {
        const error = errors[f.name as Path<V>]?.message as string | undefined
        return (
          <Field key={f.name} label={f.label} error={error}>
            {(a) =>
              f.type === 'select' ? (
                <Select {...a} invalid={!!error} {...register(f.name as Path<V>)}>
                  <option value="">Selecciona</option>
                  {f.options?.map((o) => (
                    <option key={o.value} value={o.value}>
                      {o.label}
                    </option>
                  ))}
                </Select>
              ) : (
                <Input {...a} invalid={!!error} {...register(f.name as Path<V>)} />
              )
            }
          </Field>
        )
      })}
      {errors.root && <Alert>{errors.root.message}</Alert>}
      <ModalActions className="mt-auto">
        <Button className="flex-1" onClick={onClose}>
          Cancelar
        </Button>
        <Button type="submit" variant="primary" className="flex-1" loading={isSubmitting}>
          {cta}
        </Button>
      </ModalActions>
    </Modal>
  )
}
