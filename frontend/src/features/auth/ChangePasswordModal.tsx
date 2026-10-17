import { zodResolver } from '@hookform/resolvers/zod'
import { useForm } from 'react-hook-form'
import { ApiError, apiPut } from '../../shared/api/client'
import { Alert, Button, Field, Input, Modal, ModalActions, useToast } from '../../shared/ui'
import { changePasswordSchema, type ChangePasswordValues } from './schemas/auth'

type Props = { open: boolean; onClose: () => void }

/** Cambio de contraseña desde el menú de perfil (spec §5: auth, prioridad S). */
export function ChangePasswordModal({ open, onClose }: Props) {
  const toast = useToast()
  const {
    register,
    handleSubmit,
    reset,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<ChangePasswordValues>({
    resolver: zodResolver(changePasswordSchema),
    defaultValues: { current: '', next: '', confirm: '' },
  })

  const close = () => {
    reset()
    onClose()
  }

  const onSubmit = handleSubmit(async (values) => {
    try {
      await apiPut('/auth/password', { currentPassword: values.current, newPassword: values.next })
      toast('Contraseña actualizada')
      close()
    } catch (e) {
      if (e instanceof ApiError && e.fieldErrors?.currentPassword) {
        setError('current', { message: e.fieldErrors.currentPassword })
      } else if (e instanceof ApiError && e.fieldErrors?.newPassword) {
        setError('next', { message: e.fieldErrors.newPassword })
      } else {
        setError('root', { message: 'No pudimos actualizar la contraseña.' })
      }
    }
  })

  return (
    <Modal open={open} onClose={close} title="Cambiar contraseña" onSubmit={onSubmit} width="min(400px,92%)">
      <Field label="Contraseña actual" error={errors.current?.message}>
        {(a) => <Input {...a} type="password" autoComplete="current-password" invalid={!!errors.current} {...register('current')} />}
      </Field>
      <Field label="Nueva contraseña" error={errors.next?.message}>
        {(a) => <Input {...a} type="password" autoComplete="new-password" invalid={!!errors.next} {...register('next')} />}
      </Field>
      <Field label="Confirmar nueva contraseña" error={errors.confirm?.message}>
        {(a) => <Input {...a} type="password" autoComplete="new-password" invalid={!!errors.confirm} {...register('confirm')} />}
      </Field>
      {errors.root && <Alert>{errors.root.message}</Alert>}
      <ModalActions>
        <Button onClick={close}>Cancelar</Button>
        <Button type="submit" variant="primary" loading={isSubmitting}>
          Guardar
        </Button>
      </ModalActions>
    </Modal>
  )
}
