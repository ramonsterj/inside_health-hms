import { computed, type ComputedRef, type Ref } from 'vue'
import { ADMISSION_TYPE_ORDER } from '@/constants/admissionType'
import { AdmissionType, type AdmissionListItem } from '@/types/admission'
import { Sex } from '@/types/patient'
import type { AdmissionsListGroupBy } from '@/stores/admissionsListPreferences'

const TYPE_ORDER_INDEX: Record<AdmissionType, number> = ADMISSION_TYPE_ORDER.reduce(
  (acc, type, index) => {
    // eslint-disable-next-line security/detect-object-injection -- Safe: type is iterated from a typed enum constant.
    acc[type] = index
    return acc
  },
  {} as Record<AdmissionType, number>
)

function genderOrder(item: AdmissionListItem): number {
  if (item.patient.sex === Sex.FEMALE) return 0
  if (item.patient.sex === Sex.MALE) return 1
  return 2
}

/**
 * Compares two triage codes (or absent codes). Untriaged sorts to the end;
 * present codes are compared via `localeCompare` so 'A' precedes 'B', etc.
 */
export function compareTriageCode(
  a: string | null | undefined,
  b: string | null | undefined
): number {
  const aMissing = a === null || a === undefined
  const bMissing = b === null || b === undefined
  if (aMissing && bMissing) return 0
  if (aMissing) return 1
  if (bMissing) return -1
  return a.localeCompare(b)
}

/**
 * Returns negative if `a` should sort before `b` based on triage code.
 * Items without a triage code sort to the end.
 */
export function compareTriage(a: AdmissionListItem, b: AdmissionListItem): number {
  return compareTriageCode(a.triageCode?.code, b.triageCode?.code)
}

/**
 * Stable sort by triage code (with untriaged at the end), preserving original
 * order for ties. Returns a new array.
 */
export function sortByTriage(items: AdmissionListItem[]): AdmissionListItem[] {
  return items
    .map((item, index) => ({ item, index }))
    .sort((a, b) => {
      const cmp = compareTriage(a.item, b.item)
      if (cmp !== 0) return cmp
      return a.index - b.index
    })
    .map(entry => entry.item)
}

/**
 * Formats a triage group's display label: "{code} · {description}" when the
 * description is present, otherwise just the code. Returns `null` for an
 * absent triage code, leaving the caller to render its own "untriaged" label.
 */
export function formatTriageGroupLabel(
  triageCode: { code: string; description: string | null } | null | undefined
): string | null {
  if (!triageCode) return null
  return triageCode.description?.trim()
    ? `${triageCode.code} · ${triageCode.description}`
    : triageCode.code
}

/**
 * Returns the short pill label from a (possibly localized) description string:
 * the portion before the first " - " separator (e.g.
 * "Critical - Immediate attention required" → "Critical", or
 * "Crítico - Atención inmediata requerida" → "Crítico"). Falls back to the
 * full description when no separator is present, and to `codeFallback` when
 * no description is set. Returns `null` when both are missing.
 */
export function shortLabelFromDescription(
  description: string | null | undefined,
  codeFallback?: string | null
): string | null {
  const trimmed = description?.trim()
  if (!trimmed) return codeFallback ?? null
  const dashIndex = trimmed.indexOf(' - ')
  return dashIndex > 0 ? trimmed.slice(0, dashIndex).trim() : trimmed
}

/**
 * Convenience wrapper that extracts the short pill label directly from a
 * triage code's raw `description`. Callers that have access to a localized
 * description string (e.g. via `t('triageCode.codes.{code}', ...)`) should
 * pass it to {@link shortLabelFromDescription} instead so the pill label
 * follows the active locale.
 */
export function formatTriageShortLabel(
  triageCode: { code: string; description: string | null } | null | undefined
): string | null {
  if (!triageCode) return null
  return shortLabelFromDescription(triageCode.description, triageCode.code)
}

/**
 * Reusable table-grouping helpers for the admissions list. Sorts rows so that
 * PrimeVue `DataTable` row grouping (which requires pre-sorted data) works for
 * Gender, Type, or Triage, and exposes the matching `groupRowsBy` key.
 *
 * Within each group rows are further sorted by triage code so the most-urgent
 * patients show first; ungrouped data is sorted by triage code globally.
 */
export function useAdmissionsTableGrouping(
  admissions: Ref<AdmissionListItem[]>,
  groupBy: Ref<AdmissionsListGroupBy>
) {
  const tableData: ComputedRef<AdmissionListItem[]> = computed(() => {
    const data = admissions.value
    const indexed = data.map((item, index) => ({ item, index }))
    indexed.sort((a, b) => {
      if (groupBy.value === 'gender') {
        const ga = genderOrder(a.item)
        const gb = genderOrder(b.item)
        if (ga !== gb) return ga - gb
      } else if (groupBy.value === 'type') {
        const ta = TYPE_ORDER_INDEX[a.item.type]
        const tb = TYPE_ORDER_INDEX[b.item.type]
        if (ta !== tb) return ta - tb
      }
      // Within a group (or globally for 'none'/'triage'), urgent triage first.
      const triageCmp = compareTriage(a.item, b.item)
      if (triageCmp !== 0) return triageCmp
      return a.index - b.index
    })
    return indexed.map(entry => entry.item)
  })

  const groupRowsBy: ComputedRef<string | undefined> = computed(() => {
    if (groupBy.value === 'gender') return 'patient.sex'
    if (groupBy.value === 'type') return 'type'
    if (groupBy.value === 'triage') return 'triageCode.id'
    return undefined
  })

  return { tableData, groupRowsBy }
}
