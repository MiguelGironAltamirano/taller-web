import { zodResolver } from '@hookform/resolvers/zod'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { Link, Navigate, useNavigate } from 'react-router'
import { z } from 'zod'
import { ApiError, apiPost } from '../../shared/api/client'
import type { RegisterBody } from '../../shared/api/types'
import { AuthCard, FieldError, buttonClass, inputClass } from '../../shared/ui/AuthCard'
import { useAuth } from './auth-context'

const DOMAIN = '@unmsm.edu.pe'
const PASSWORD_MESSAGE = 'La contraseña debe tener entre 8 y 72 caracteres, con al menos una letra y un dígito'
const NAME_MESSAGE = 'El nombre debe tener entre 3 y 150 caracteres'

const schema = z.object({
  fullName: z.string().trim().min(3, NAME_MESSAGE).max(150, NAME_MESSAGE),
  email: z
    .string()
    .trim()
    .toLowerCase()
    .refine((value) => value.endsWith(DOMAIN) && value.length > DOMAIN.length, `El correo debe terminar en ${DOMAIN}`),
  password: z
    .string()
    .min(8, PASSWORD_MESSAGE)
    .max(72, PASSWORD_MESSAGE)
    .regex(/[A-Za-z]/, PASSWORD_MESSAGE)
    .regex(/\d/, PASSWORD_MESSAGE),
  requesterType: z.enum(['ESTUDIANTE', 'DOCENTE', 'ADMINISTRATIVO']),
})
type RegisterForm = z.infer<typeof schema>

export function RegisterPage() {
  const { user } = useAuth()
  const navigate = useNavigate()
  const [formError, setFormError] = useState<string | null>(null)
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<RegisterForm>({ resolver: zodResolver(schema), defaultValues: { requesterType: 'ESTUDIANTE' } })

  if (user) return <Navigate to="/" replace />

  async function onSubmit(values: RegisterForm) {
    setFormError(null)
    try {
      const body: RegisterBody = values
      await apiPost('/auth/register', body)
      void navigate('/login', { replace: true, state: { message: 'Cuenta creada. Ahora puedes iniciar sesión.' } })
    } catch (error) {
      setFormError(
        error instanceof ApiError && error.status === 409
          ? 'El correo ya está registrado'
          : 'No se pudo crear la cuenta. Inténtalo de nuevo.',
      )
    }
  }

  return (
    <AuthCard title="Crear cuenta">
      <form onSubmit={(event) => void handleSubmit(onSubmit)(event)} noValidate className="space-y-4">
        <div>
          <label htmlFor="fullName" className="text-sm font-medium text-slate-700">
            Nombre completo
          </label>
          <input id="fullName" autoComplete="name" className={inputClass} {...register('fullName')} />
          <FieldError message={errors.fullName?.message} />
        </div>
        <div>
          <label htmlFor="email" className="text-sm font-medium text-slate-700">
            Correo institucional
          </label>
          <input id="email" type="email" autoComplete="email" className={inputClass} {...register('email')} />
          <FieldError message={errors.email?.message} />
        </div>
        <div>
          <label htmlFor="password" className="text-sm font-medium text-slate-700">
            Contraseña
          </label>
          <input id="password" type="password" autoComplete="new-password" className={inputClass} {...register('password')} />
          <FieldError message={errors.password?.message} />
        </div>
        <div>
          <label htmlFor="requesterType" className="text-sm font-medium text-slate-700">
            Tipo de solicitante
          </label>
          <select id="requesterType" className={inputClass} {...register('requesterType')}>
            <option value="ESTUDIANTE">Estudiante</option>
            <option value="DOCENTE">Docente</option>
            <option value="ADMINISTRATIVO">Administrativo</option>
          </select>
        </div>
        <FieldError message={formError ?? undefined} />
        <button type="submit" disabled={isSubmitting} className={buttonClass}>
          Crear cuenta
        </button>
      </form>
      <p className="mt-4 text-sm text-slate-600">
        ¿Ya tienes cuenta?{' '}
        <Link to="/login" className="font-medium text-slate-900 underline">
          Inicia sesión
        </Link>
      </p>
    </AuthCard>
  )
}
