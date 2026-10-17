import { z } from 'zod'
import { PRIORITIES } from '../../../shared/domain'

export const DESCRIPTION_MIN = 20
export const DESCRIPTION_MAX = 2000

/** Validación del formulario "Nueva solicitud" (spec §5.1). */
export const newRequestSchema = z.object({
  category: z.string().min(1, 'Selecciona una categoría.'),
  faculty: z.string().min(1, 'Selecciona la facultad.'),
  pavilion: z.string().min(1, 'Selecciona el pabellón.'),
  room: z.string().min(1, 'Selecciona el ambiente.'),
  description: z
    .string()
    .superRefine((value, ctx) => {
      if (value.length > DESCRIPTION_MAX) {
        ctx.addIssue({ code: 'custom', message: `Máximo ${DESCRIPTION_MAX} caracteres.` })
      } else if (value.trim().length < DESCRIPTION_MIN) {
        ctx.addIssue({
          code: 'custom',
          message: `La descripción debe tener al menos ${DESCRIPTION_MIN} caracteres (${value.trim().length}/${DESCRIPTION_MIN}).`,
        })
      }
    }),
  priority: z.enum(PRIORITIES, { error: 'Selecciona una prioridad.' }),
})

export type NewRequestValues = z.infer<typeof newRequestSchema>
