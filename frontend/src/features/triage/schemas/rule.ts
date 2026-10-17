import { z } from 'zod'

/** `chispa, humo` → `['chispa', 'humo']` */
export const parseKeywords = (value: string): string[] =>
  value
    .split(',')
    .map((k) => k.trim())
    .filter(Boolean)

export const ruleSchema = z.object({
  keywords: z.string().refine((v) => parseKeywords(v).length > 0, 'Ingresa al menos una palabra clave.'),
  categoryId: z.string().min(1, 'Selecciona la categoría.'),
  priorityId: z.string().min(1, 'Selecciona la prioridad.'),
})

export type RuleValues = z.infer<typeof ruleSchema>
