import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createI18n } from 'vue-i18n'
import PrimeVue from 'primevue/config'
import ConfirmationService from 'primevue/confirmationservice'
import en from '@/i18n/locales/en.json'
import es from '@/i18n/locales/es.json'
import { AdmissionStatus } from '@/types/admission'
import { useAuthStore } from '@/stores/auth'
import ProgressNoteList from './ProgressNoteList.vue'
import ClinicalHistoryView from './ClinicalHistoryView.vue'
import DocumentList from '@/components/documents/DocumentList.vue'
import PsychotherapyActivityList from '@/components/psychotherapy/PsychotherapyActivityList.vue'
import { useProgressNoteStore } from '@/stores/progressNote'
import { useClinicalHistoryStore } from '@/stores/clinicalHistory'
import { useDocumentStore } from '@/stores/document'
import { usePsychotherapyActivityStore } from '@/stores/psychotherapyActivity'
import { usePsychotherapyCategoryStore } from '@/stores/psychotherapyCategory'

// useErrorHandler pulls in PrimeVue's useToast — stub it so mounts stay light.
vi.mock('@/composables/useErrorHandler', () => ({
  useErrorHandler: () => ({ showError: vi.fn(), showSuccess: vi.fn() })
}))

function i18n() {
  return createI18n({ legacy: false, locale: 'en', fallbackLocale: 'en', messages: { en, es } })
}

function setAuth(permissions: string[]) {
  const auth = useAuthStore()
  auth.$patch({ user: { roles: ['ADMIN'], permissions } } as never)
}

async function mountComponent(
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  component: any,
  status: AdmissionStatus,
  permissions: string[]
) {
  setActivePinia(createPinia())
  setAuth(permissions)

  // Seed each store so onMounted fetches are no-ops and getters return empty.
  const progress = useProgressNoteStore()
  vi.spyOn(progress, 'fetchProgressNotes').mockResolvedValue(undefined as never)
  vi.spyOn(progress, 'getProgressNotes').mockReturnValue([])
  vi.spyOn(progress, 'getTotalNotes').mockReturnValue(0)

  const clinical = useClinicalHistoryStore()
  vi.spyOn(clinical, 'fetchClinicalHistory').mockResolvedValue(undefined as never)
  vi.spyOn(clinical, 'getClinicalHistory').mockReturnValue(undefined)

  const documents = useDocumentStore()
  vi.spyOn(documents, 'fetchDocuments').mockResolvedValue([] as never)
  vi.spyOn(documents, 'getDocuments').mockReturnValue([])

  const psycho = usePsychotherapyActivityStore()
  vi.spyOn(psycho, 'fetchActivities').mockResolvedValue(undefined as never)
  vi.spyOn(psycho, 'getActivities').mockReturnValue([])

  // The activity form dialog fetches categories on mount; stub it so no real request fires.
  const categories = usePsychotherapyCategoryStore()
  vi.spyOn(categories, 'fetchActiveCategories').mockResolvedValue(undefined as never)

  const wrapper = mount(component, {
    props: { admissionId: 1, admissionStatus: status },
    global: { plugins: [PrimeVue, ConfirmationService, i18n()] }
  })
  await flushPromises()
  return wrapper
}

describe('discharge protection — write affordances are hidden on a discharged admission', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('ProgressNoteList: shows "Add Note" when ACTIVE, hides it when DISCHARGED', async () => {
    const active = await mountComponent(ProgressNoteList, AdmissionStatus.ACTIVE, [
      'progress-note:create'
    ])
    expect(active.text()).toContain('Add Note')

    const discharged = await mountComponent(ProgressNoteList, AdmissionStatus.DISCHARGED, [
      'progress-note:create'
    ])
    expect(discharged.text()).not.toContain('Add Note')
  })

  it('ClinicalHistoryView: shows "Create Clinical History" when ACTIVE, hides it when DISCHARGED', async () => {
    const active = await mountComponent(ClinicalHistoryView, AdmissionStatus.ACTIVE, [
      'clinical-history:create'
    ])
    expect(active.text()).toContain('Create Clinical History')

    const discharged = await mountComponent(ClinicalHistoryView, AdmissionStatus.DISCHARGED, [
      'clinical-history:create'
    ])
    expect(discharged.text()).not.toContain('Create Clinical History')
  })

  it('DocumentList: shows "Upload" when ACTIVE, hides it when DISCHARGED', async () => {
    const active = await mountComponent(DocumentList, AdmissionStatus.ACTIVE, [
      'admission:upload-documents'
    ])
    expect(active.text()).toContain('Upload')

    const discharged = await mountComponent(DocumentList, AdmissionStatus.DISCHARGED, [
      'admission:upload-documents'
    ])
    expect(discharged.text()).not.toContain('Upload')
  })

  it('PsychotherapyActivityList: shows "Add Activity" when ACTIVE, hides it when DISCHARGED', async () => {
    const active = await mountComponent(PsychotherapyActivityList, AdmissionStatus.ACTIVE, [
      'psychotherapy-activity:create'
    ])
    expect(active.text()).toContain('Add Activity')

    const discharged = await mountComponent(PsychotherapyActivityList, AdmissionStatus.DISCHARGED, [
      'psychotherapy-activity:create'
    ])
    expect(discharged.text()).not.toContain('Add Activity')
  })
})
