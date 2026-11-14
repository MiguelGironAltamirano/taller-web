import { zodResolver } from '@hookform/resolvers/zod'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { Link, useLocation, useNavigate } from 'react-router'
import { ApiError } from '../../shared/api/client'
import { Alert, Avatar, Button, Field, Icon, Input, PasswordInput, useToast } from '../../shared/ui'
import { AuthShell } from './AuthShell'
import { useAuth } from './context'
import { DEMO_ACCOUNTS, SHOW_DEMO_ACCOUNTS } from './demoAccounts'
import { loginSchema, type LoginValues } from './schemas/auth'

export function LoginPage() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const toast = useToast()
  const [formError, setFormError] = useState('')
  const prefill = (location.state as { email?: string } | null)?.email ?? ''

  const {
    register,
    handleSubmit,
    setValue,
    clearErrors,
    formState: { errors, isSubmitting },
  } = useForm<LoginValues>({
    resolver: zodResolver(loginSchema),
    defaultValues: { email: prefill, password: '' },
  })

  const onSubmit = handleSubmit(async (values) => {
    setFormError('')
    try {
      await login(values.email, values.password)
      const from = (location.state as { from?: string } | null)?.from
      toast('Sesión iniciada')
      navigate(from && from !== '/login' ? from : '/', { replace: true })
    } catch (e) {
      setFormError(e instanceof ApiError ? e.detail || e.title : 'No pudimos conectar con el servidor. Revisa tu conexión e inténtalo de nuevo.')
    }
  })

  return (
    <AuthShell>
      <form onSubmit={onSubmit} noValidate className="flex flex-col gap-5">
        <div>
          <span className="inline-flex items-center gap-1.5 rounded-full border border-line bg-surface px-2.5 py-1 text-xs font-medium text-muted shadow-sm">
            <Icon name="lock" size={13} strokeWidth={2} className="text-primary-fg" />
            Acceso institucional
          </span>
          <h1 className="m-0 mt-4 text-[30px] font-bold leading-tight tracking-tight">Iniciar sesión</h1>
          <p className="m-0 mt-2 text-[14.5px] leading-relaxed text-muted">
            Te damos la bienvenida. Ingresa con tu cuenta institucional.
          </p>
        </div>
        {formError && <Alert>{formError}</Alert>}
        <div className="flex flex-col gap-4">
          <Field label="Correo" error={errors.email?.message}>
            {(a) => (
              <Input
                {...a}
                icon="mail"
                type="email"
                autoComplete="username"
                placeholder="usuario@unmsm.edu.pe"
                className="h-12 text-[14.5px]"
                {...register('email', { onChange: () => setFormError('') })}
              />
            )}
          </Field>
          <div className="flex flex-col gap-1.5">
            <Field label="Contraseña" error={errors.password?.message}>
              {(a) => (
                <PasswordInput
                  {...a}
                  icon="lock"
                  autoComplete="current-password"
                  placeholder="••••••••"
                  className="h-12 text-[14.5px]"
                  {...register('password', { onChange: () => setFormError('') })}
                />
              )}
            </Field>
            <button
              type="button"
              className="self-end border-0 bg-transparent p-0 text-[13px] font-medium text-primary-fg hover:underline"
              onClick={() => toast('Te enviaremos un enlace de recuperación a tu correo.', { tone: 'info' })}
            >
              ¿Olvidaste tu contraseña?
            </button>
          </div>
        </div>
        <Button
          type="submit"
          variant="primary"
          size="lg"
          loading={isSubmitting}
          className="group h-12 w-full text-[15px] shadow-[0_8px_20px_-8px_var(--primary)]"
        >
          {isSubmitting ? 'Ingresando…' : 'Ingresar'}
          {!isSubmitting && (
            <Icon name="arrowR" size={17} strokeWidth={2.25} className="transition-transform group-hover:translate-x-0.5" />
          )}
        </Button>
        <div className="flex items-center gap-3 text-xs text-muted">
          <span className="h-px flex-1 bg-line" />
          ¿Primera vez aquí?
          <span className="h-px flex-1 bg-line" />
        </div>
        <Link
          to="/registro"
          className="inline-flex h-11 items-center justify-center gap-2 rounded-lg border border-line bg-surface font-semibold text-fg no-underline shadow-[0_1px_2px_rgb(0_0_0/0.05)] transition-colors hover:border-line-strong hover:bg-surface2 hover:no-underline"
        >
          Crear cuenta
        </Link>

        {SHOW_DEMO_ACCOUNTS && (
          <section aria-labelledby="demo-title" className="mt-2 rounded-xl border border-dashed border-line-strong p-3.5">
            <h2 id="demo-title" className="m-0 mb-2.5 text-xs font-semibold uppercase tracking-wider text-muted">
              Cuentas de ejemplo
            </h2>
            <div className="grid grid-cols-1 gap-1.5 sm:grid-cols-2">
              {DEMO_ACCOUNTS.map((u) => (
                <button
                  key={u.email}
                  type="button"
                  onClick={() => {
                    setValue('email', u.email)
                    setValue('password', u.password)
                    clearErrors()
                    setFormError('')
                  }}
                  className="flex items-center gap-2.5 rounded-lg border border-line bg-surface px-2.5 py-2 text-left text-fg hover:border-line-strong hover:bg-surface2"
                >
                  <Avatar name={u.name} size="sm" tone="soft" />
                  <span className="min-w-0 flex-1">
                    <span className="block truncate text-[13px] font-medium">{u.name}</span>
                    <span className="block truncate text-xs text-muted">{u.role}</span>
                  </span>
                </button>
              ))}
            </div>
          </section>
        )}
      </form>
    </AuthShell>
  )
}
