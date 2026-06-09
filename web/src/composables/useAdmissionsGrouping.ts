import { ADMISSION_TYPE_ORDER } from '@/constants/admissionType'
import { AdmissionType, type AdmissionListItem } from '@/types/admission'
import { Sex } from '@/types/patient'
import type { AdmissionsListGroupBy } from '@/stores/admissionsListPreferences'

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
function compareTriage(a: AdmissionListItem, b: AdmissionListItem): number {
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
 *
 * Per the i18n reference-data standard (docs/architecture/I18N.md), callers
 * pass the locale-aware description from `useCodeLabels().triageCodeLabel(code,
 * raw)`; the raw `triageCode.description` is only a last-resort fallback for
 * admin-created codes not yet in the bundle.
 */
export function formatTriageGroupLabel(
  triageCode: { code: string; description: string | null } | null | undefined,
  localizedDescription?: string | null
): string | null {
  if (!triageCode) return null
  const description = (localizedDescription ?? triageCode.description)?.trim()
  return description ? `${triageCode.code} · ${description}` : triageCode.code
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
 * A single, ordered, non-empty card group produced by {@link bucketAdmissions}.
 * The same descriptor shape is used at both grouping levels (primary outer
 * panels and secondary inner panels).
 */
export interface CardGroupDescriptor {
  /** Stable identifier (used as Panel key). */
  key: string
  /** Translated label for the group header. */
  label: string
  /** Drives the icon/dot rendering on the group header. */
  kind: 'gender' | 'type' | 'triage'
  /** For gender groups: which sex to render in the icon. */
  sex?: Sex | null
  /** For type groups: which type to render in the dot. */
  type?: AdmissionType
  /** For triage groups: badge color (null for untriaged). */
  triageColor?: string | null
  /** For triage groups: the triage code letter (null for untriaged). */
  triageCode?: string | null
  /** Group members, triage-sorted (most-urgent first, untriaged last). */
  items: AdmissionListItem[]
}

/**
 * i18n-aware label resolvers injected into {@link bucketAdmissions} so the pure
 * bucketing logic stays free of vue-i18n and remains unit-testable.
 */
export interface BucketLabelers {
  /** Localized header label for a gender bucket. */
  genderLabel: (sex: Sex | null) => string
  /** Localized header label for an admission type bucket. */
  typeLabel: (type: AdmissionType) => string
  /**
   * Localized header label for a triage bucket. `triageCode` is `null` for the
   * untriaged bucket.
   */
  triageLabel: (triageCode: { code: string; description: string | null } | null) => string
}

/**
 * Buckets a flat list of admissions along a single dimension into ordered,
 * non-empty {@link CardGroupDescriptor}s. Items within each bucket are
 * triage-sorted so the innermost (leaf) grid stays urgency-ordered regardless
 * of which level it is rendered at.
 *
 * Ordering rules mirror the original inline card grouping:
 * - **gender** → female, male, other (other only when present);
 * - **type** → `ADMISSION_TYPE_ORDER`, empty buckets dropped;
 * - **triage** → `compareTriageCode` (untriaged last).
 */
export function bucketAdmissions(
  items: AdmissionListItem[],
  dimension: Exclude<AdmissionsListGroupBy, 'none'>,
  labelers: BucketLabelers
): CardGroupDescriptor[] {
  if (dimension === 'gender') {
    const female: AdmissionListItem[] = []
    const male: AdmissionListItem[] = []
    const other: AdmissionListItem[] = []
    for (const item of items) {
      if (item.patient.sex === Sex.FEMALE) female.push(item)
      else if (item.patient.sex === Sex.MALE) male.push(item)
      else other.push(item)
    }
    const result: CardGroupDescriptor[] = []
    if (female.length > 0) {
      result.push({
        key: 'female',
        label: labelers.genderLabel(Sex.FEMALE),
        kind: 'gender',
        sex: Sex.FEMALE,
        items: sortByTriage(female)
      })
    }
    if (male.length > 0) {
      result.push({
        key: 'male',
        label: labelers.genderLabel(Sex.MALE),
        kind: 'gender',
        sex: Sex.MALE,
        items: sortByTriage(male)
      })
    }
    if (other.length > 0) {
      result.push({
        key: 'other',
        label: labelers.genderLabel(null),
        kind: 'gender',
        sex: null,
        items: sortByTriage(other)
      })
    }
    return result
  }

  if (dimension === 'type') {
    const buckets = new Map<AdmissionType, AdmissionListItem[]>()
    for (const item of items) {
      const list = buckets.get(item.type) ?? []
      list.push(item)
      buckets.set(item.type, list)
    }
    return ADMISSION_TYPE_ORDER.flatMap((type): CardGroupDescriptor[] => {
      const group = buckets.get(type)
      if (!group || group.length === 0) return []
      return [
        {
          key: type,
          label: labelers.typeLabel(type),
          kind: 'type',
          type,
          items: sortByTriage(group)
        }
      ]
    })
  }

  // dimension === 'triage' — map keyed by triage id; key `null` collects untriaged.
  const buckets = new Map<number | null, AdmissionListItem[]>()
  for (const item of items) {
    const id = item.triageCode?.id ?? null
    const list = buckets.get(id) ?? []
    list.push(item)
    buckets.set(id, list)
  }
  // Sort triage groups by code (untriaged last).
  const entries = [...buckets.entries()].sort(([, listA], [, listB]) =>
    compareTriageCode(listA[0]?.triageCode?.code, listB[0]?.triageCode?.code)
  )
  return entries.map(([id, list]): CardGroupDescriptor => {
    const tc = list[0]?.triageCode ?? null
    if (id === null || !tc) {
      return {
        key: 'triage-none',
        label: labelers.triageLabel(null),
        kind: 'triage',
        triageColor: null,
        triageCode: null,
        items: sortByTriage(list)
      }
    }
    return {
      key: `triage-${id}`,
      label: labelers.triageLabel(tc),
      kind: 'triage',
      triageColor: tc.color,
      triageCode: tc.code,
      items: sortByTriage(list)
    }
  })
}
