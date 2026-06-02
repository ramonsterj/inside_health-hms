import { describe, it, expect, beforeEach, vi } from 'vitest'
import { defineComponent, h, ref } from 'vue'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createI18n } from 'vue-i18n'
import en from '@/i18n/locales/en.json'
import es from '@/i18n/locales/es.json'
import { useMedicalRecordSummary, type VisibleSections } from './useMedicalRecordSummary'
import { useClinicalHistoryStore } from '@/stores/clinicalHistory'
import { useProgressNoteStore } from '@/stores/progressNote'
import { useMedicalOrderStore } from '@/stores/medicalOrder'
import { useNursingNoteStore } from '@/stores/nursingNote'
import { useVitalSignStore } from '@/stores/vitalSign'
import { useDocumentStore } from '@/stores/document'
import { usePsychotherapyActivityStore } from '@/stores/psychotherapyActivity'
import { MedicalOrderStatus, MedicalOrderCategory } from '@/types/medicalRecord'
import type { AdmissionDetail } from '@/types/admission'

function i18n() {
  return createI18n({ legacy: false, locale: 'en', fallbackLocale: 'en', messages: { en, es } })
}

const ALL_VISIBLE: VisibleSections = {
  clinicalHistory: true,
  progressNotes: true,
  medicalOrders: true,
  nursingNotes: true,
  vitalSigns: true,
  documents: true,
  consulting: true,
  psychotherapyActivities: true
}

// Spy every fetch method to a resolved no-op so the immediate prefetch never hits the network.
function stubFetches() {
  vi.spyOn(useClinicalHistoryStore(), 'fetchClinicalHistory').mockResolvedValue(null)
  vi.spyOn(useProgressNoteStore(), 'fetchProgressNotesSummary').mockResolvedValue()
  vi.spyOn(useMedicalOrderStore(), 'fetchMedicalOrders').mockResolvedValue({} as never)
  vi.spyOn(useNursingNoteStore(), 'fetchNursingNotesSummary').mockResolvedValue()
  vi.spyOn(useVitalSignStore(), 'fetchVitalSignsSummary').mockResolvedValue()
  vi.spyOn(useDocumentStore(), 'fetchDocuments').mockResolvedValue([])
  vi.spyOn(usePsychotherapyActivityStore(), 'fetchActivities').mockResolvedValue([])
}

async function runComposable(visible: VisibleSections, admission: AdmissionDetail | null = null) {
  let api!: ReturnType<typeof useMedicalRecordSummary>
  const Comp = defineComponent({
    setup() {
      api = useMedicalRecordSummary(ref(1), ref(visible), ref(admission))
      return () => h('div')
    }
  })
  const wrapper = mount(Comp, { global: { plugins: [i18n()] } })
  await flushPromises()
  return { wrapper, api }
}

describe('useMedicalRecordSummary', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('derives metrics/severity/updated from seeded store getters', async () => {
    stubFetches()
    useClinicalHistoryStore().clinicalHistories.set(1, {
      id: 9,
      admissionId: 1,
      updatedAt: '2026-05-09T14:30:00',
      createdAt: '2026-05-01T10:00:00'
    } as never)
    useProgressNoteStore().totalNotes.set(1, 6)
    useProgressNoteStore().progressNotes.set(1, [
      { id: 1, createdAt: '2026-06-01T08:15:00' } as never
    ])
    useVitalSignStore().vitalSigns.set(1, [
      { id: 1, systolicBp: 120, diastolicBp: 80, recordedAt: '2026-06-01T06:00:00' } as never
    ])
    useDocumentStore().documents.set(1, [
      { id: 1, createdAt: '2026-05-28T11:20:00' } as never,
      { id: 2, createdAt: '2026-05-29T09:00:00' } as never
    ])

    const { api } = await runComposable(ALL_VISIBLE)
    const s = api.summary.value

    expect(s.clinicalHistory).toEqual({
      metric: 'Complete',
      severity: 'success',
      updated: '09/05/2026 - 14:30'
    })
    expect(s.progressNotes?.metric).toBe('6 notes')
    expect(s.progressNotes?.severity).toBe('info')
    expect(s.progressNotes?.updated).toBe('01/06/2026 - 08:15')
    expect(s.vitalSigns?.metric).toBe('BP 120/80')
    expect(s.vitalSigns?.severity).toBe('success')
    expect(s.documents?.metric).toBe('2 files')
    // latest of the two document dates wins
    expect(s.documents?.updated).toBe('29/05/2026 - 09:00')
  })

  it('counts ACTIVA orders across every category', async () => {
    stubFetches()
    useMedicalOrderStore().medicalOrders.set(1, {
      [MedicalOrderCategory.ORDENES_MEDICAS]: [{ status: MedicalOrderStatus.ACTIVA }],
      [MedicalOrderCategory.DIETA]: [
        { status: MedicalOrderStatus.ACTIVA },
        { status: MedicalOrderStatus.SOLICITADO }
      ],
      [MedicalOrderCategory.MEDICAMENTOS]: [{ status: MedicalOrderStatus.AUTORIZADO }]
    } as never)

    const { api } = await runComposable({ medicalOrders: true })
    expect(api.summary.value.medicalOrders?.metric).toBe('2 active')
    expect(api.summary.value.medicalOrders?.severity).toBe('warn')
  })

  it('derives the consulting metric from the admission prop', async () => {
    stubFetches()
    const admission = {
      consultingPhysicians: [
        { id: 1, createdAt: '2026-05-30T09:10:00' },
        { id: 2, createdAt: '2026-05-31T09:10:00' }
      ]
    } as unknown as AdmissionDetail

    const { api } = await runComposable({ consulting: true }, admission)
    expect(api.summary.value.consulting?.metric).toBe('2 consultations')
    expect(api.summary.value.consulting?.updated).toBe('31/05/2026 - 09:10')
  })

  it('yields bare cards (undefined metric) for empty stores', async () => {
    stubFetches()
    const { api } = await runComposable(ALL_VISIBLE, {
      consultingPhysicians: []
    } as unknown as AdmissionDetail)
    const s = api.summary.value

    // Count/latest-based sections render no metric when empty.
    expect(s.progressNotes?.metric).toBeUndefined()
    expect(s.medicalOrders?.metric).toBeUndefined()
    expect(s.nursingNotes?.metric).toBeUndefined()
    expect(s.vitalSigns?.metric).toBeUndefined()
    expect(s.documents?.metric).toBeUndefined()
    expect(s.consulting?.metric).toBeUndefined()
    expect(s.psychotherapyActivities?.metric).toBeUndefined()
    // Clinical history with no record also stays bare.
    expect(s.clinicalHistory?.metric).toBeUndefined()
  })

  it('only includes visible sections in the summary map', async () => {
    stubFetches()
    const { api } = await runComposable({ clinicalHistory: true, vitalSigns: true })
    const keys = Object.keys(api.summary.value)
    expect(keys).toContain('clinicalHistory')
    expect(keys).toContain('vitalSigns')
    expect(keys).not.toContain('progressNotes')
    expect(keys).not.toContain('documents')
  })

  it('a rejected prefetch does not throw and other sections still resolve', async () => {
    const clinical = useClinicalHistoryStore()
    vi.spyOn(clinical, 'fetchClinicalHistory').mockRejectedValue(new Error('boom'))
    vi.spyOn(useProgressNoteStore(), 'fetchProgressNotesSummary').mockResolvedValue()
    vi.spyOn(useVitalSignStore(), 'fetchVitalSignsSummary').mockResolvedValue()

    // Seed a vital sign so its (independent) section still produces a metric.
    useVitalSignStore().vitalSigns.set(1, [
      { id: 1, systolicBp: 118, diastolicBp: 76, recordedAt: '2026-06-01T06:00:00' } as never
    ])

    const { api } = await runComposable({
      clinicalHistory: true,
      progressNotes: true,
      vitalSigns: true
    })

    // No throw on mount + the unaffected section is still populated.
    expect(api.summary.value.vitalSigns?.metric).toBe('BP 118/76')
    expect(api.summary.value.clinicalHistory?.metric).toBeUndefined()
  })
})
