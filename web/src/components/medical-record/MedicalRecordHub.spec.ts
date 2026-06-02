import { describe, it, expect, beforeEach, vi } from 'vitest'
import { ref } from 'vue'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createI18n } from 'vue-i18n'
import PrimeVue from 'primevue/config'
import en from '@/i18n/locales/en.json'
import es from '@/i18n/locales/es.json'
import { AdmissionStatus, AdmissionType } from '@/types/admission'
import { useAuthStore } from '@/stores/auth'
import MedicalRecordHub from './MedicalRecordHub.vue'

// Stub the summary composable — metrics are exercised in useMedicalRecordSummary.spec.ts.
vi.mock('@/composables/useMedicalRecordSummary', () => ({
  useMedicalRecordSummary: () => ({ summary: ref({}), prefetch: vi.fn() })
}))

function i18n() {
  return createI18n({ legacy: false, locale: 'en', fallbackLocale: 'en', messages: { en, es } })
}

function setAuth(roles: string[], permissions: string[]) {
  const auth = useAuthStore()
  auth.$patch({ user: { roles, permissions } } as never)
}

const CHILD_STUBS = {
  ClinicalHistoryView: true,
  ProgressNoteList: true,
  MedicalOrderList: true,
  NursingNoteList: true,
  VitalSignTable: true,
  VitalSignCharts: true,
  DocumentList: true,
  ConsultingPhysiciansPanel: true,
  PsychotherapyActivityList: true
}

async function mountHub(opts: {
  roles?: string[]
  permissions?: string[]
  status?: AdmissionStatus
  type?: AdmissionType
}) {
  setActivePinia(createPinia())
  setAuth(opts.roles ?? ['ADMIN'], opts.permissions ?? [])

  const wrapper = mount(MedicalRecordHub, {
    props: {
      admissionId: 1,
      admissionType: opts.type ?? AdmissionType.HOSPITALIZATION,
      admissionStatus: opts.status ?? AdmissionStatus.ACTIVE,
      consultingPhysicians: []
    },
    global: { plugins: [PrimeVue, i18n()], stubs: CHILD_STUBS }
  })
  await flushPromises()
  return wrapper
}

describe('MedicalRecordHub', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('renders a section card per permitted section for an admin', async () => {
    const wrapper = await mountHub({ roles: ['ADMIN'] })
    for (const key of [
      'clinicalHistory',
      'progressNotes',
      'medicalOrders',
      'nursingNotes',
      'vitalSigns',
      'documents',
      'consulting',
      'psychotherapyActivities'
    ]) {
      expect(wrapper.find(`[data-testid="section-card-${key}"]`).exists()).toBe(true)
    }
  })

  it('opens on the grid with no default-open section (no back control)', async () => {
    const wrapper = await mountHub({ roles: ['ADMIN'] })
    expect(wrapper.find('.section-grid').exists()).toBe(true)
    expect(wrapper.find('[data-testid="section-back"]').exists()).toBe(false)
  })

  it('drills into a section and back returns to the grid', async () => {
    const wrapper = await mountHub({ roles: ['ADMIN'] })

    await wrapper.find('[data-testid="section-card-clinicalHistory"]').trigger('click')
    expect(wrapper.find('[data-testid="section-back"]').exists()).toBe(true)
    expect(wrapper.find('.section-grid').exists()).toBe(false)
    expect(wrapper.findComponent({ name: 'ClinicalHistoryView' }).exists()).toBe(true)

    await wrapper.find('[data-testid="section-back"]').trigger('click')
    expect(wrapper.find('.section-grid').exists()).toBe(true)
    expect(wrapper.find('[data-testid="section-back"]').exists()).toBe(false)
  })

  it('shows the discharged banner only when discharged', async () => {
    const active = await mountHub({ roles: ['ADMIN'], status: AdmissionStatus.ACTIVE })
    expect(active.find('.discharged-banner').exists()).toBe(false)

    const discharged = await mountHub({ roles: ['ADMIN'], status: AdmissionStatus.DISCHARGED })
    expect(discharged.find('.discharged-banner').exists()).toBe(true)
  })

  it('gates the psychotherapy card behind a HOSPITALIZATION admission', async () => {
    const hosp = await mountHub({
      roles: ['PSYCHOLOGIST'],
      permissions: ['psychotherapy-activity:read'],
      type: AdmissionType.HOSPITALIZATION
    })
    expect(hosp.find('[data-testid="section-card-psychotherapyActivities"]').exists()).toBe(true)

    const ambulatory = await mountHub({
      roles: ['PSYCHOLOGIST'],
      permissions: ['psychotherapy-activity:read'],
      type: AdmissionType.AMBULATORY
    })
    expect(ambulatory.find('[data-testid="section-card-psychotherapyActivities"]').exists()).toBe(
      false
    )
  })

  it('only shows section cards the user has permission to view', async () => {
    const wrapper = await mountHub({
      roles: ['NURSE'],
      permissions: ['nursing-note:read', 'vital-sign:read']
    })
    expect(wrapper.find('[data-testid="section-card-nursingNotes"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="section-card-vitalSigns"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="section-card-clinicalHistory"]').exists()).toBe(false)
    expect(wrapper.find('[data-testid="section-card-medicalOrders"]').exists()).toBe(false)
  })
})
