export function formatPrice(value: number | null | undefined): string {
  if (value == null) return '-'
  return `Q${value.toFixed(2)}`
}
