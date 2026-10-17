import { zodResolver } from '@hookform/resolvers/zod'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { Link, useLocation, useNavigate } from 'react-router'
import { ApiError } from '../../shared/api/client'
import { Alert, Button, Field, Input, useToast } from '../../shared/ui'
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
    formState: { isSubmitting },
  } = useForm<LoginValues>({
    resolver: zodResolver(loginSchema),
    defaultValues: { email: prefill, password: '' },
  })

  const onInvalid = () => setFormError('Ingresa tu correo y contraseña.')

  const onSubmit = handleSubmit(async (values) => {
    setFormError('')
    try {
      await login(values.email, values.password)
      const from = (location.state as { from?: string } | null)?.from
      toast('Sesión iniciada')
      navigate(from && from !== '/login' ? from : '/', { replace: true })
    } catch (e) {
      setFormError(e instanceof ApiError ? e.detail || e.title : 'No pudimos conectar con el servidor.')
    }
  }, onInvalid)

  return (
    <AuthShell>
      <form onSubmit={onSubmit} noValidate className="flex w-full max-w-[360px] flex-col gap-3.5">
        <div>
          <h1 className="m-0 mb-1 text-2xl font-semibold">Iniciar sesión</h1>
          <div className="text-muted">Usa tu cuenta institucional.</div>
        </div>
        {formError && <Alert>{formError}</Alert>}
        <Field label="Correo">
          {(a) => (
            <Input
              {...a}
              type="email"
              autoComplete="username"
              placeholder="usuario@unmsm.edu.pe"
              {...register('email', { onChange: () => setFormError('') })}
            />
          )}
        </Field>
        <Field label="Contraseña">
          {(a) => (
            <Input
              {...a}
              type="password"
              autoComplete="current-password"
              {...register('password', { onChange: () => setFormError('') })}
            />
          )}
        </Field>
        <Button type="submit" variant="primary" size="lg" loading={isSubmitting}>
          {isSubmitting ? 'Ingresando…' : 'Ingresar'}
        </Button>
        <div className="flex justify-between text-[13px]">
          <button
            type="button"
            className="border-0 bg-transparent p-0 text-primary-fg hover:underline"
            onClick={() => toast('Te enviaremos un enlace de recuperación a tu correo.')}
          >
            ¿Olvidaste tu contraseña?
          </button>
          <Link to="/registro">Crear cuenta</Link>
        </div>
        {SHOW_DEMO_ACCOUNTS && (
          <div className="border-t border-line pt-3.5">
            <div className="mb-2 text-xs text-muted">Cuentas de ejemplo</div>
            <div className="flex flex-col gap-1.5">
              {DEMO_ACCOUNTS.map((u) => (
                <button
                  key={u.email}
                  type="button"
                  onClick={() => {
                    setValue('email', u.email)
                    setValue('password', u.password)
                    setFormError('')
                  }}
                  className="flex items-center gap-2.5 rounded-lg border border-line bg-surface px-2.5 py-2 text-left text-fg hover:bg-surface2"
                >
                  <span className="grid size-7 place-items-center rounded-full bg-primary-soft text-xs font-semibold text-primary-fg">
                    {u.initials}
                  </span>
                  <span className="min-w-0 flex-1">
                    <span className="block text-[13px] font-medium">{u.name}</span>
                    <span className="block text-xs text-muted">{u.role}</span>
                  </span>
                </button>
              ))}
            </div>
          </div>
        )}
      </form>
    </AuthShell>
  )
}
