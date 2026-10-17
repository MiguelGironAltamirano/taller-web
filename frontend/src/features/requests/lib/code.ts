const CODE_PATTERN = /^SOL-\d{4}-\d{6}$/

/** Código `SOL-AAAA-NNNNNN`: correlativo por año, único e inmutable (spec §5.1). */
export function formatRequestCode(year: number, sequence: number): string {
  return `SOL-${year}-${String(sequence).padStart(6, '0')}`
}

export function isRequestCode(value: string): boolean {
  return CODE_PATTERN.test(value)
}
