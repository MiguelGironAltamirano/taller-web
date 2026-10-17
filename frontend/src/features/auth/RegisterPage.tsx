import { zodResolver } from '@hookform/resolvers/zod'
import { Controller, useForm, useWatch } from 'react-hook-form'
import { Link, useNavigate } from 'react-router'
import { ApiError, apiPost } from '../../shared/api/client'
import { requesterTypeToApi } from '../../shared/api/mappers'
import { REQUESTER_TYPES } from '../../shared/domain'
import { cn } from '../../shared/lib/cn'
import { Button, Field, Input, useToast } from '../../shared/ui'
import { AuthShell } from './AuthShell'
import { passwordScore, registerSchema, type RegisterValues } from './schemas/auth'

const STRENGTH = [
  { color: 'var(--border)', label: '' },
  { color: '#dc2626', label: 'Débil' },
  { color: '#d97706', label: 'Regular' },
  { color: '#65a30d', label: 'Buena' },
  { color: '#16a34a', label: 'Fuerte' },
]

// Campos del contrato de la API → campos del formulario.
const FIELD_MAP: Record<string, keyof RegisterValues> = {
  fullName: 'name',
  email: 'email',
  password: 'password',
  requesterType: 'type',
}

export function RegisterPage() {
  const navigate = useNavigate()
  const toast = useToast()
  const {
    register,
    control,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<RegisterValues>({
    resolver: zodResolver(registerSchema),
    defaultValues: { name: '', email: '', type: 'Estudiante', password: '', confirm: '' },
  })
  const score = passwordScore(useWatch({ control, name: 'password' }) ?? '')
  const strength = STRENGTH[score]

  const onSubmit = handleSubmit(async (values) => {
    try {
      await apiPost('/auth/register', {
        fullName: values.name.trim(),
        email: values.email.trim(),
        password: values.password,
        requesterType: requesterTypeToApi(values.type),
      })
      toast('Cuenta creada. Ya puedes ingresar.')
      navigate('/login', { state: { email: values.email.trim() } })
    } catch (e) {
      if (e instanceof ApiError && e.fieldErrors) {
        for (const [field, message] of Object.entries(e.fieldErrors)) {
          const mapped = FIELD_MAP[field]
          if (mapped) setError(mapped, { message })
        }
      } else {
        toast('No pudimos crear la cuenta. Inténtalo de nuevo.', { bad: true })
      }
    }
  })

  return (
    <AuthShell>
      <form onSubmit={onSubmit} noValidate className="flex w-full max-w-[440px] flex-col gap-3.5 self-start">
        <div>
          <h1 className="m-0 mb-1 text-2xl font-semibold">Crear cuenta</h1>
          <div className="text-muted">Solo correos @unmsm.edu.pe.</div>
        </div>
        <Field label="Nombre completo" error={errors.name?.message}>
          {(a) => <Input {...a} invalid={!!errors.name} autoComplete="name" {...register('name')} />}
        </Field>
        <Field label="Correo institucional" error={errors.email?.message}>
          {(a) => (
            <Input
              {...a}
              type="email"
              invalid={!!errors.email}
              autoComplete="email"
              placeholder="usuario@unmsm.edu.pe"
              {...register('email')}
            />
          )}
        </Field>
        <div className="flex flex-col gap-1.5 text-[13px] font-medium" role="group" aria-label="Tipo de usuario">
          Tipo de usuario
          <Controller
            control={control}
            name="type"
            render={({ field }) => (
              <div className="flex flex-wrap gap-2">
                {REQUESTER_TYPES.map((t) => (
                  <button
                    key={t}
                    type="button"
                    aria-pressed={field.value === t}
                    onClick={() => field.onChange(t)}
                    className={cn(
                      'h-9 rounded-lg border px-3.5 font-medium max-md:h-11',
                      field.value === t ? 'border-primary bg-primary-soft text-primary-fg' : 'border-line bg-surface text-fg',
                    )}
                  >
                    {t}
                  </button>
                ))}
              </div>
            )}
          />
        </div>
        <Field label="Contraseña" error={errors.password?.message}>
          {(a) => (
            <>
              <Input {...a} type="password" invalid={!!errors.password} autoComplete="new-password" {...register('password')} />
              <span className="flex items-center gap-2 font-normal">
                <span className="h-1.5 flex-1 overflow-hidden rounded-full bg-surface2">
                  <span
                    className="block h-full transition-[width]"
                    style={{ width: `${score * 25}%`, background: strength.color }}
                  />
                </span>
                <span className="min-w-[60px] text-muted">{strength.label}</span>
              </span>
            </>
          )}
        </Field>
        <Field label="Confirmar contraseña" error={errors.confirm?.message}>
          {(a) => <Input {...a} type="password" invalid={!!errors.confirm} autoComplete="new-password" {...register('confirm')} />}
        </Field>
        <Button type="submit" variant="primary" size="lg" loading={isSubmitting}>
          Crear cuenta
        </Button>
        <div className="text-center text-[13px]">
          ¿Ya tienes cuenta? <Link to="/login">Ingresar</Link>
        </div>
      </form>
    </AuthShell>
  )
}
