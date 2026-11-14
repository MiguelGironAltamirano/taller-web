import { zodResolver } from '@hookform/resolvers/zod'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { Link, useLocation, useNavigate } from 'react-router'
import { ApiError } from '../../shared/api/client'
import { Alert, Avatar, Button, Field, Input, PasswordInput, useToast } from '../../shared/ui'
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
      <form onSubmit={onSubmit} noValidate className="flex flex-col gap-4">
        <div>
          <h1 className="m-0 text-[26px] font-semibold tracking-tight">Iniciar sesión</h1>
          <p className="m-0 mt-1.5 text-muted">Ingresa con tu cuenta institucional @unmsm.edu.pe.</p>
        </div>
        {formError && <Alert>{formError}</Alert>}
        <Field label="Correo" error={errors.email?.message}>
          {(a) => (
            <Input
              {...a}
              type="email"
              autoComplete="username"
              placeholder="usuario@unmsm.edu.pe"
              className="h-11"
              {...register('email', { onChange: () => setFormError('') })}
            />
          )}
        </Field>
        <Field label="Contraseña" error={errors.password?.message}>
          {(a) => (
            <PasswordInput
              {...a}
              autoComplete="current-password"
              className="h-11"
              {...register('password', { onChange: () => setFormError('') })}
            />
          )}
        </Field>
        <div className="-mt-1 flex justify-end text-[13px]">
          <button
            type="button"
            className="border-0 bg-transparent p-0 font-medium text-primary-fg hover:underline"
            onClick={() => toast('Te enviaremos un enlace de recuperación a tu correo.', { tone: 'info' })}
          >
            ¿Olvidaste tu contraseña?
          </button>
        </div>
        <Button type="submit" variant="primary" size="lg" loading={isSubmitting} className="h-11 w-full">
          {isSubmitting ? 'Ingresando…' : 'Ingresar'}
        </Button>
        <p className="m-0 text-center text-[13px] text-muted">
          ¿Aún no tienes cuenta?{' '}
          <Link to="/registro" className="font-medium">
            Crear cuenta
          </Link>
        </p>

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
