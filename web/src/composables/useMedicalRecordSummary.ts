import { computed, watch, type Ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useClinicalHistoryStore } from '@/stores/clinicalHistory'
import { useProgressNoteStore } from '@/stores/progressNote'
import { useMedicalOrderStore } from '@/stores/medicalOrder'
import { useNursingNoteStore } from '@/stores/nursingNote'
import { useVitalSignStore } from '@/stores/vitalSign'
import { useDocumentStore } from '@/stores/document'
import { usePsychotherapyActivityStore } from '@/stores/psychotherapyActivity'
import { formatDateTime } from '@/utils/format'
import { MedicalOrderStatus } from '@/types/medicalRecord'
import type { AdmissionDetail } from '@/types/admission'

export type MetricSeverity = 'success' | 'info' | 'warn' | 'secondary' | 'danger'

export interface SectionSummary {
  metric?: string
  severity?: MetricSeverity
  updated?: string
}

/** Sections the hub can render. Keyed identically to the hub's card grid. */
export type SummarySectionKey =
  | 'clinicalHistory'
  | 'progressNotes'
  | 'medicalOrders'
  | 'nursingNotes'
  | 'vitalSigns'
  | 'documents'
  | 'consulting'
  | 'psychotherapyActivities'

export type VisibleSections = Partial<Record<SummarySectionKey, boolean>>

/**
 * Prefetches lightweight summary data for the visible medical-record sections and exposes a
 * reactive per-section metric/severity/updated map for the section-hub cards.
 *
 * Prefetch fans out one request per visible section via Promise.allSettled with a per-section
 * try/catch, so a single failure can never blank the hub — the affected card simply renders
 * without a metric (graceful degradation). Paginated sections (progress notes, nursing notes,
 * vital signs) use a dedicated size=1 *summary* fetch that writes a separate summary cache, never
 * the list map a drilled-in view owns — so a late-resolving prefetch can't truncate the full list.
 *
 * Metrics are derived from store getters (not fetch return values). The `getLatest*` getters prefer
 * the live list when a drilled-in section has loaded it (staying reactive to creates/edits) and
 * fall back to the summary cache otherwise.
 */
export function useMedicalRecordSummary(
  admissionId: Ref<number>,
  visibleSections: Ref<VisibleSections>,
  admission: Ref<AdmissionDetail | null | undefined>
) {
  const { t } = useI18n()

  const clinicalHistoryStore = useClinicalHistoryStore()
  const progressNoteStore = useProgressNoteStore()
  const medicalOrderStore = useMedicalOrderStore()
  const nursingNoteStore = useNursingNoteStore()
  const vitalSignStore = useVitalSignStore()
  const documentStore = useDocumentStore()
  const psychotherapyActivityStore = usePsychotherapyActivityStore()

  // Returns the most-recent date among the values, already display-formatted, or undefined.
  function latestFormatted(values: Array<string | null | undefined>): string | undefined {
    const present = values.filter((v): v is string => !!v)
    if (present.length === 0) return undefined
    return formatDateTime(present.reduce((a, b) => (a > b ? a : b)))
  }

  const summary = computed<Record<string, SectionSummary>>(() => {
    const id = admissionId.value
    const visible = visibleSections.value
    const map: Record<string, SectionSummary> = {}

    if (visible.clinicalHistory) {
      const history = clinicalHistoryStore.getClinicalHistory(id)
      map.clinicalHistory = history
        ? {
            metric: t('medicalRecord.metrics.complete'),
            severity: 'success',
            updated: formatDateTime(history.updatedAt || history.createdAt)
          }
        : {}
    }

    if (visible.progressNotes) {
      const total = progressNoteStore.getTotalNotes(id)
      const latest = progressNoteStore.getLatestNote(id)
      map.progressNotes =
        total > 0
          ? {
              metric: t('medicalRecord.metrics.notes', { count: total }),
              severity: 'info',
              updated: latest ? formatDateTime(latest.createdAt) : undefined
            }
          : {}
    }

    if (visible.medicalOrders) {
      const grouped = medicalOrderStore.getMedicalOrders(id)
      const orders = grouped ? Object.values(grouped).flat() : []
      const active = orders.filter(o => o.status === MedicalOrderStatus.ACTIVA).length
      map.medicalOrders =
        active > 0
          ? {
              metric: t('medicalRecord.metrics.activeOrders', { count: active }),
              severity: 'warn',
              updated: latestFormatted(orders.map(o => o.updatedAt || o.createdAt))
            }
          : {}
    }

    if (visible.nursingNotes) {
      const total = nursingNoteStore.getTotalNotes(id)
      const latest = nursingNoteStore.getLatestNote(id)
      map.nursingNotes =
        total > 0
          ? {
              metric: t('medicalRecord.metrics.notes', { count: total }),
              severity: 'info',
              updated: latest ? formatDateTime(latest.createdAt) : undefined
            }
          : {}
    }

    if (visible.vitalSigns) {
      const latest = vitalSignStore.getLatestVitalSign(id)
      map.vitalSigns = latest
        ? {
            metric: t('medicalRecord.metrics.bloodPressure', {
              value: `${latest.systolicBp}/${latest.diastolicBp}`
            }),
            severity: 'success',
            updated: formatDateTime(latest.recordedAt)
          }
        : {}
    }

    if (visible.documents) {
      const docs = documentStore.getDocuments(id)
      map.documents =
        docs.length > 0
          ? {
              metric: t('medicalRecord.metrics.files', { count: docs.length }),
              severity: 'secondary',
              updated: latestFormatted(docs.map(d => d.createdAt))
            }
          : {}
    }

    if (visible.consulting) {
      const consultations = admission.value?.consultingPhysicians ?? []
      map.consulting =
        consultations.length > 0
          ? {
              metric: t('medicalRecord.metrics.consultations', { count: consultations.length }),
              severity: 'info',
              updated: latestFormatted(consultations.map(c => c.createdAt))
            }
          : {}
    }

    if (visible.psychotherapyActivities) {
      const activities = psychotherapyActivityStore.getActivities(id)
      map.psychotherapyActivities =
        activities.length > 0
          ? {
              metric: t('medicalRecord.metrics.sessions', { count: activities.length }),
              severity: 'info',
              updated: latestFormatted(activities.map(a => a.createdAt))
            }
          : {}
    }

    return map
  })

  async function runSafely(task: () => Promise<unknown>): Promise<void> {
    try {
      await task()
    } catch {
      // Silent degradation — a failed prefetch just leaves that card without a metric.
    }
  }

  async function prefetch(): Promise<void> {
    const id = admissionId.value
    if (!id) return
    const visible = visibleSections.value
    const tasks: Array<Promise<unknown>> = []

    // Consulting needs no fetch — it is sourced from the admission prop.
    if (visible.clinicalHistory) {
      tasks.push(runSafely(() => clinicalHistoryStore.fetchClinicalHistory(id)))
    }
    if (visible.progressNotes) {
      tasks.push(runSafely(() => progressNoteStore.fetchProgressNotesSummary(id)))
    }
    if (visible.medicalOrders) {
      tasks.push(runSafely(() => medicalOrderStore.fetchMedicalOrders(id)))
    }
    if (visible.nursingNotes) {
      tasks.push(runSafely(() => nursingNoteStore.fetchNursingNotesSummary(id)))
    }
    if (visible.vitalSigns) {
      tasks.push(runSafely(() => vitalSignStore.fetchVitalSignsSummary(id)))
    }
    if (visible.documents) {
      tasks.push(runSafely(() => documentStore.fetchDocuments(id)))
    }
    if (visible.psychotherapyActivities) {
      tasks.push(runSafely(() => psychotherapyActivityStore.fetchActivities(id)))
    }

    await Promise.allSettled(tasks)
  }

  watch([admissionId, visibleSections], () => void prefetch(), { immediate: true, deep: true })

  return { summary, prefetch }
}
