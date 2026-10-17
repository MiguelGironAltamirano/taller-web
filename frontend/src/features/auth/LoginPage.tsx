import { zodResolver } from '@hookform/resolvers/zod'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { Link, Navigate, useLocation, useNavigate } from 'react-router'
import { z } from 'zod'
import { ApiError } from '../../shared/api/client'
import { AuthCard, FieldError, buttonClass, inputClass } from '../../shared/ui/AuthCard'
import { useAuth } from './auth-context'

const schema = z.object({
  email: z.string().trim().min(1, 'Ingresa tu correo').email('Ingresa un correo válido'),
  password: z.string().min(1, 'Ingresa tu contraseña'),
})
type LoginForm = z.infer<typeof schema>

export function LoginPage() {
  const { user, login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const notice = (location.state as { message?: string } | null)?.message
  const [formError, setFormError] = useState<string | null>(null)
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<LoginForm>({ resolver: zodResolver(schema) })

  if (user) return <Navigate to="/" replace />

  async function onSubmit(values: LoginForm) {
    setFormError(null)
    try {
      await login(values)
      void navigate('/', { replace: true })
    } catch (error) {
      setFormError(
        error instanceof ApiError && error.status === 401
          ? 'Credenciales inválidas'
          : 'No se pudo iniciar sesión. Inténtalo de nuevo.',
      )
    }
  }

  return (
    <AuthCard title="Iniciar sesión">
      {notice && (
        <p role="status" className="mb-4 rounded bg-green-50 p-3 text-sm text-green-800">
          {notice}
        </p>
      )}
      <form onSubmit={(event) => void handleSubmit(onSubmit)(event)} noValidate className="space-y-4">
        <div>
          <label htmlFor="email" className="text-sm font-medium text-slate-700">
            Correo institucional
          </label>
          <input id="email" type="email" autoComplete="username" className={inputClass} {...register('email')} />
          <FieldError message={errors.email?.message} />
        </div>
        <div>
          <label htmlFor="password" className="text-sm font-medium text-slate-700">
            Contraseña
          </label>
          <input id="password" type="password" autoComplete="current-password" className={inputClass} {...register('password')} />
          <FieldError message={errors.password?.message} />
        </div>
        <FieldError message={formError ?? undefined} />
        <button type="submit" disabled={isSubmitting} className={buttonClass}>
          Iniciar sesión
        </button>
      </form>
      <p className="mt-4 text-sm text-slate-600">
        ¿No tienes cuenta?{' '}
        <Link to="/registro" className="font-medium text-slate-900 underline">
          Regístrate
        </Link>
      </p>
    </AuthCard>
  )
}
