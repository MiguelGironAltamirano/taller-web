const cell = (value: string | number) => {
  const text = String(value)
  return /[",\n]/.test(text) ? `"${text.replaceAll('"', '""')}"` : text
}

/** Arma un CSV escapando comas, comillas y saltos de línea. */
export function buildCsv(header: string[], rows: (string | number)[][]): string {
  return [header, ...rows].map((line) => line.map(cell).join(',')).join('\n')
}
