import { z } from 'zod'

const MAX_KEYWORDS = 20
const MIN_KEYWORD = 2
const MAX_KEYWORD = 50

/** `chispa, humo` → `['chispa', 'humo']` */
export const parseKeywords = (value: string): string[] =>
  value
    .split(',')
    .map((k) => k.trim())
    .filter(Boolean)

export const ruleSchema = z.object({
  keywords: z
    .string()
    .refine((v) => parseKeywords(v).length > 0, 'Ingresa al menos una palabra clave.')
    .refine((v) => parseKeywords(v).length <= MAX_KEYWORDS, `Ingresa como máximo ${MAX_KEYWORDS} palabras clave.`)
    .refine(
      (v) => parseKeywords(v).every((k) => k.length >= MIN_KEYWORD && k.length <= MAX_KEYWORD),
      `Cada palabra clave debe tener entre ${MIN_KEYWORD} y ${MAX_KEYWORD} caracteres.`,
    ),
  categoryId: z.string().min(1, 'Selecciona la categoría.'),
  priorityId: z.string().min(1, 'Selecciona la prioridad.'),
})

export type RuleValues = z.infer<typeof ruleSchema>
