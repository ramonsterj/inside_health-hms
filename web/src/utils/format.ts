// Guatemala hospital display formats:
//   date: dd/MM/yyyy
//   time: HH:mm (24h)
//   datetime: dd/MM/yyyy - HH:mm
// Intentionally locale-independent so output never depends on the browser locale.

function pad2(n: number): string {
  return n < 10 ? `0${n}` : String(n)
}

// Date-only ISO strings (yyyy-MM-dd) are parsed as local midnight to avoid the
// UTC-shift trap where `new Date("2026-05-09")` would render as 2026-05-08 in
// timezones west of UTC (Guatemala is UTC-6).
const DATE_ONLY_RE = /^(\d{4})-(\d{2})-(\d{2})$/

function parseDate(value: string | Date | null | undefined): Date | null {
  if (value == null || value === '') return null
  if (value instanceof Date) {
    return Number.isNaN(value.getTime()) ? null : value
  }
  const dateOnly = DATE_ONLY_RE.exec(value)
  if (dateOnly) {
    const [, y, m, d] = dateOnly
    return new Date(Number(y), Number(m) - 1, Number(d))
  }
  const d = new Date(value)
  return Number.isNaN(d.getTime()) ? null : d
}

// Public helper for round-tripping API date strings (`yyyy-MM-dd`) into a Date
// that a <DatePicker> can bind to without the UTC-shift trap.
export function fromApiDate(value: string | Date | null | undefined): Date | null {
  return parseDate(value)
}

export function formatDate(value: string | Date | null | undefined): string {
  const d = parseDate(value)
  if (!d) return '-'
  return `${pad2(d.getDate())}/${pad2(d.getMonth() + 1)}/${d.getFullYear()}`
}

export function formatTime(value: string | Date | null | undefined): string {
  const d = parseDate(value)
  if (!d) return '-'
  return `${pad2(d.getHours())}:${pad2(d.getMinutes())}`
}

export function formatDateTime(value: string | Date | null | undefined): string {
  const d = parseDate(value)
  if (!d) return '-'
  return `${formatDate(d)} - ${formatTime(d)}`
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

export function formatPrice(value: number | null | undefined): string {
  if (value == null) return '-'
  return `Q${value.toFixed(2)}`
}

export function formatCurrency(value: number | null | undefined): string {
  if (value == null) return 'Q 0.00'
  return new Intl.NumberFormat('es-GT', { style: 'currency', currency: 'GTQ' }).format(value)
}

export function toApiDate(value: Date): string
export function toApiDate(value: Date | string | null | undefined): string | null
export function toApiDate(value: Date | string | null | undefined): string | null {
  if (!value) return null
  if (value instanceof Date) {
    return `${value.getFullYear()}-${pad2(value.getMonth() + 1)}-${pad2(value.getDate())}`
  }
  return value
}
