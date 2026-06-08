import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createI18n } from 'vue-i18n'
import PrimeVue from 'primevue/config'
import en from '@/i18n/locales/en.json'
import es from '@/i18n/locales/es.json'
import { AdmissionStatus } from '@/types/admission'
import { useAuthStore } from '@/stores/auth'
import { useClinicalHistoryStore } from '@/stores/clinicalHistory'
import ClinicalHistoryView from './ClinicalHistoryView.vue'
import type { ClinicalHistoryResponse } from '@/types/medicalRecord'

vi.mock('@/composables/useErrorHandler', () => ({
  useErrorHandler: () => ({ showError: vi.fn(), showSuccess: vi.fn() })
}))

function i18n() {
  return createI18n({ legacy: false, locale: 'en', fallbackLocale: 'en', messages: { en, es } })
}

function seededHistory(): ClinicalHistoryResponse {
  return {
    id: 1,
    admissionId: 1,
    reasonForAdmission: '<p><strong>Bold reason</strong></p>',
    historyOfPresentIllness: null,
    psychiatricHistory: null,
    medicalHistory: null,
    familyHistory: null,
    personalHistory: null,
    substanceUseHistory: null,
    legalHistory: null,
    socialHistory: null,
    developmentalHistory: null,
    educationalOccupationalHistory: null,
    sexualHistory: null,
    religiousSpiritualHistory: null,
    mentalStatusExam: null,
    physicalExam: null,
    diagnosticImpression: null,
    treatmentPlan: null,
    riskAssessment: null,
    prognosis: null,
    informedConsentNotes: null,
    additionalNotes: null,
    createdAt: '2026-05-01T10:00:00',
    updatedAt: '2026-05-09T14:30:00',
    createdBy: null,
    updatedBy: null
  }
}

async function mountView(opts: {
  status: AdmissionStatus
  permissions: string[]
  history?: ClinicalHistoryResponse
}) {
  setActivePinia(createPinia())
  const auth = useAuthStore()
  auth.$patch({ user: { roles: ['MEDICO'], permissions: opts.permissions } } as never)

  const store = useClinicalHistoryStore()
  vi.spyOn(store, 'fetchClinicalHistory').mockResolvedValue(opts.history ?? null)
  if (opts.history) {
    store.clinicalHistories.set(1, opts.history)
  }

  const wrapper = mount(ClinicalHistoryView, {
    props: { admissionId: 1, admissionStatus: opts.status },
    global: { plugins: [PrimeVue, i18n()] }
  })
  await flushPromises()
  return wrapper
}

describe('ClinicalHistoryView (card grid)', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('renders each field group as a card', async () => {
    const wrapper = await mountView({
      status: AdmissionStatus.ACTIVE,
      permissions: ['clinical-history:read'],
      history: seededHistory()
    })
    // Six field-section groups -> six cards.
    expect(wrapper.findAll('.history-card')).toHaveLength(6)
  })

  it('renders sanitized rich-text content via v-html', async () => {
    const wrapper = await mountView({
      status: AdmissionStatus.ACTIVE,
      permissions: ['clinical-history:read'],
      history: seededHistory()
    })
    const value = wrapper.find('.field-value')
    expect(value.find('strong').text()).toBe('Bold reason')
  })

  it('dims groups that have no content', async () => {
    const wrapper = await mountView({
      status: AdmissionStatus.ACTIVE,
      permissions: ['clinical-history:read'],
      history: seededHistory()
    })
    // Only the presentation group has content; the other five are dimmed.
    expect(wrapper.findAll('.history-card.no-content')).toHaveLength(5)
  })

  it('shows the Edit button when ACTIVE with update permission, hides it when discharged', async () => {
    const active = await mountView({
      status: AdmissionStatus.ACTIVE,
      permissions: ['clinical-history:read', 'clinical-history:update'],
      history: seededHistory()
    })
    expect(active.find('.view-header').text()).toContain('Edit')

    const discharged = await mountView({
      status: AdmissionStatus.DISCHARGED,
      permissions: ['clinical-history:read', 'clinical-history:update'],
      history: seededHistory()
    })
    expect(
      discharged.find('.view-header').exists() ? discharged.find('.view-header').text() : ''
    ).not.toContain('Edit')
  })

  it('shows the Create button in the empty state for creators (ACTIVE only)', async () => {
    const active = await mountView({
      status: AdmissionStatus.ACTIVE,
      permissions: ['clinical-history:create']
    })
    expect(active.text()).toContain('Create Clinical History')

    const discharged = await mountView({
      status: AdmissionStatus.DISCHARGED,
      permissions: ['clinical-history:create']
    })
    expect(discharged.text()).not.toContain('Create Clinical History')
  })
})
