import { z } from 'zod'
import { INSTITUTIONAL_DOMAIN } from '../../../shared/domain'

const institutional = new RegExp(`^[^\\s@]+@${INSTITUTIONAL_DOMAIN.replace('.', '\\.')}$`, 'i')

/** Usuario: el área es obligatoria para técnico y supervisor (spec §4). */
export const userSchema = z
  .object({
    fullName: z.string().trim().min(1, 'Ingresa el nombre.').min(3, 'El nombre debe tener entre 3 y 150 caracteres.').max(150, 'El nombre debe tener entre 3 y 150 caracteres.'),
    email: z.string().trim().refine((v) => institutional.test(v), 'Usa un correo @unmsm.edu.pe.'),
    role: z.enum(['SOLICITANTE', 'TECNICO', 'SUPERVISOR', 'ADMINISTRADOR']),
    areaId: z.string(),
  })
  .refine((v) => !(v.role === 'TECNICO' || v.role === 'SUPERVISOR') || v.areaId !== '', {
    message: 'El área es obligatoria para este rol.',
    path: ['areaId'],
  })
export type UserFormValues = z.infer<typeof userSchema>

export const areaSchema = z.object({ name: z.string().trim().min(1, 'Ingresa el nombre.') })
export type AreaFormValues = z.infer<typeof areaSchema>

export const categorySchema = z.object({
  name: z.string().trim().min(1, 'Ingresa el nombre.'),
  areaId: z.string().min(1, 'Selecciona el área.'),
})
export type CategoryFormValues = z.infer<typeof categorySchema>

export const prioritySchema = z.object({
  slaHours: z.string().refine((v) => /^\d+$/.test(v) && Number(v) > 0 && Number(v) <= 8760, 'Ingresa horas entre 1 y 8760.'),
})
export type PriorityFormValues = z.infer<typeof prioritySchema>

export const locationSchema = z.object({
  faculty: z.string().trim().min(1, 'Ingresa la facultad.'),
  pavilion: z.string().trim().min(1, 'Ingresa el pabellón.'),
  room: z.string().trim().min(1, 'Ingresa el ambiente.'),
})
export type LocationFormValues = z.infer<typeof locationSchema>

export const configSchema = z.object({
  autoCloseHours: z.string().refine((v) => /^\d+$/.test(v) && Number(v) > 0, 'Ingresa horas mayores a 0.'),
})
