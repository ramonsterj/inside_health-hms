export function formatPrice(value: number | null | undefined): string {
  if (value == null) return '-'
  return `Q${value.toFixed(2)}`
}

export function formatCurrency(value: number): string {
  return new Intl.NumberFormat('es-GT', { style: 'currency', currency: 'GTQ' }).format(value)
}
