import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import type { VueWrapper } from '@vue/test-utils'
import { createTestingPinia } from '@pinia/testing'
import { createI18n } from 'vue-i18n'
import PrimeVue from 'primevue/config'
import ConfirmationService from 'primevue/confirmationservice'
import AdmissionDetailView from './AdmissionDetailView.vue'
import en from '@/i18n/locales/en.json'
import es from '@/i18n/locales/es.json'
import api from '@/services/api'
import { useAuthStore } from '@/stores/auth'
import { useAdmissionStore } from '@/stores/admission'
import { AdmissionStatus, AdmissionType } from '@/types/admission'

vi.mock('@/services/api', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn()
  }
}))

vi.mock('@/composables/useErrorHandler', () => ({
  useErrorHandler: () => ({
    showError: vi.fn(),
    showSuccess: vi.fn()
  })
}))

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: vi.fn(), back: vi.fn() }),
  useRoute: () => ({ params: { id: '5' } })
}))

const mockedApi = api as unknown as { get: ReturnType<typeof vi.fn> }

// Stub the shared RichTextEditor (Quill doesn't initialise reliably under happy-dom):
// a plain textarea that keeps the `#discharge-note` id and the v-model contract, so the
// parent's validation/submit logic stays under test without depending on Quill internals.
const RichTextEditorStub = {
  props: ['modelValue'],
  emits: ['update:modelValue'],
  template:
    '<textarea id="discharge-note" :value="modelValue" ' +
    '@input="$emit(\'update:modelValue\', $event.target.value)"></textarea>'
}

const activeAdmission = {
  id: 5,
  patient: { id: 1, firstName: 'Juan', lastName: 'Perez', sex: 'MALE', hasActiveAdmission: true },
  triageCode: null,
  room: null,
  treatingPhysician: {
    id: 10,
    salutation: 'DR',
    firstName: 'Maria',
    lastName: 'Garcia',
    username: 'doctor'
  },
  resident: {
    id: 20,
    salutation: 'DRA',
    firstName: 'Andrea',
    lastName: 'Pineda',
    username: 'resident'
  },
  admissionDate: '2026-01-23T10:30:00',
  dischargeDate: null,
  dischargeNote: null,
  status: AdmissionStatus.ACTIVE,
  type: AdmissionType.HOSPITALIZATION,
  inventory: null,
  hasConsentDocument: false,
  consultingPhysicians: [],
  createdAt: null,
  createdBy: null,
  updatedAt: null,
  updatedBy: null
}

const i18n = createI18n({ legacy: false, locale: 'en', fallbackLocale: 'en', messages: { en, es } })

async function mountView(roles: string[]) {
  mockedApi.get.mockResolvedValue({ data: { success: true, data: activeAdmission } })

  const pinia = createTestingPinia({ stubActions: false })
  const authStore = useAuthStore()
  authStore.user = {
    id: roles.includes('MEDICO_RESIDENTE') ? 20 : 99,
    username: 'tester',
    roles,
    permissions: ['admission:update', 'admission:discharge']
  } as never

  const admissionStore = useAdmissionStore()
  const dischargeSpy = vi
    .spyOn(admissionStore, 'dischargePatient')
    .mockResolvedValue(activeAdmission as never)

  const wrapper = mount(AdmissionDetailView, {
    global: {
      plugins: [pinia, i18n, PrimeVue, ConfirmationService],
      stubs: {
        teleport: true,
        AdmissionHeroHeader: true,
        MedicalRecordHub: true,
        AdmissionExportButton: true,
        AddConsultingPhysicianDialog: true,
        DocumentUploadDialog: true,
        DocumentViewer: true,
        AuditInfo: true,
        RichTextEditor: RichTextEditorStub
      }
    }
  })
  await flushPromises()
  return { wrapper, dischargeSpy }
}

function findButtonByText(wrapper: VueWrapper, text: string) {
  return wrapper.findAll('button').filter(b => b.text().includes(text))
}

function openDischargeButton(wrapper: VueWrapper) {
  const button = findButtonByText(wrapper, 'Discharge Patient')[0]
  if (!button) throw new Error('discharge button not found')
  return button
}

function confirmDischargeButton(wrapper: VueWrapper) {
  const buttons = findButtonByText(wrapper, 'Discharge Patient')
  const button = buttons[buttons.length - 1]
  if (!button) throw new Error('confirm button not found')
  return button
}

describe('AdmissionDetailView discharge dialog', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('opens the discharge dialog when the discharge button is clicked', async () => {
    const { wrapper } = await mountView(['PERSONAL_ADMINISTRATIVO'])

    // Header discharge button opens the dialog (no dialog content before).
    expect(wrapper.find('#discharge-note').exists()).toBe(false)
    await openDischargeButton(wrapper).trigger('click')
    await flushPromises()
    expect(wrapper.find('#discharge-note').exists()).toBe(true)
  })

  it('blocks a resident from confirming with an empty note', async () => {
    const { wrapper, dischargeSpy } = await mountView(['MEDICO_RESIDENTE'])

    await openDischargeButton(wrapper).trigger('click')
    await flushPromises()

    // Confirm (footer) button is the last "Discharge Patient" button.
    await confirmDischargeButton(wrapper).trigger('click')
    await flushPromises()

    expect(dischargeSpy).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('A comment is required when discharging the patient')
  })

  it('blocks an admin from confirming with an empty note (mandatory for everyone)', async () => {
    const { wrapper, dischargeSpy } = await mountView(['ADMINISTRADOR'])

    await openDischargeButton(wrapper).trigger('click')
    await flushPromises()

    await confirmDischargeButton(wrapper).trigger('click')
    await flushPromises()

    expect(dischargeSpy).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('A comment is required when discharging the patient')
  })

  it('sends the trimmed note when a resident provides one', async () => {
    const { wrapper, dischargeSpy } = await mountView(['MEDICO_RESIDENTE'])

    await openDischargeButton(wrapper).trigger('click')
    await flushPromises()

    await wrapper.find('#discharge-note').setValue('  Stable, follow up  ')

    await confirmDischargeButton(wrapper).trigger('click')
    await flushPromises()

    expect(dischargeSpy).toHaveBeenCalledWith(5, 'Stable, follow up')
  })

  it('sends the trimmed note when an admin provides one', async () => {
    const { wrapper, dischargeSpy } = await mountView(['ADMINISTRADOR'])

    await openDischargeButton(wrapper).trigger('click')
    await flushPromises()

    await wrapper.find('#discharge-note').setValue('  Cleared by attending  ')

    await confirmDischargeButton(wrapper).trigger('click')
    await flushPromises()

    expect(dischargeSpy).toHaveBeenCalledWith(5, 'Cleared by attending')
  })
})
