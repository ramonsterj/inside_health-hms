export function formatDateTime(dateString: string | null): string {
  if (!dateString) return '-'
  return new Date(dateString).toLocaleString()
}

export function formatDate(dateString: string | null): string {
  if (!dateString) return '-'
  return new Date(dateString).toLocaleDateString()
}

export function formatTime(dateString: string | null): string {
  if (!dateString) return '-'
  return new Date(dateString).toLocaleTimeString(undefined, {
    hour: '2-digit',
    minute: '2-digit'
  })
}

export function formatStaffName(
  staff: { salutation?: string | null; firstName?: string | null; lastName?: string | null } | null,
  t: (key: string) => string
): string {
  if (!staff) return '-'
  const salutationLabel = staff.salutation ? t(`user.salutations.${staff.salutation}`) : ''
  const fullName = `${staff.firstName || ''} ${staff.lastName || ''}`.trim()
  return `${salutationLabel} ${fullName}`.trim() || '-'
}

export function getContrastColor(hexColor: string): string {
  const r = parseInt(hexColor.slice(1, 3), 16)
  const g = parseInt(hexColor.slice(3, 5), 16)
  const b = parseInt(hexColor.slice(5, 7), 16)
  const luminance = (0.299 * r + 0.587 * g + 0.114 * b) / 255
  return luminance > 0.5 ? '#000000' : '#FFFFFF'
}

export function getFullName(firstName: string | null, lastName: string | null): string {
  return `${firstName || ''} ${lastName || ''}`.trim()
}

export function formatShortDateTime(dateString: string, locale: string): string {
  return new Date(dateString).toLocaleDateString(locale, {
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  })
}

export function formatPrice(value: number | null | undefined): string {
  if (value == null) return '-'
  return `Q${value.toFixed(2)}`
}

export function formatCurrency(value: number): string {
  return new Intl.NumberFormat('es-GT', { style: 'currency', currency: 'GTQ' }).format(value)
}

export function toApiDate(value: Date | string | null | undefined): string | null {
  if (!value) return null
  if (value instanceof Date) return value.toISOString().substring(0, 10)
  return value
}
