type ClassValue = string | false | null | undefined

/** Une clases condicionales ignorando los valores falsos. */
export function cn(...values: ClassValue[]): string {
  return values.filter(Boolean).join(' ')
}
