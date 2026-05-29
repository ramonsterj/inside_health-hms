import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createTestingPinia } from '@pinia/testing'
import { createI18n } from 'vue-i18n'
import PrimeVue from 'primevue/config'
import AdmissionFormView from './AdmissionFormView.vue'
import en from '@/i18n/locales/en.json'
import es from '@/i18n/locales/es.json'
import api from '@/services/api'
import { AdmissionType } from '@/types/admission'

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

const routerPush = vi.fn()
vi.mock('vue-router', () => ({
  useRouter: () => ({ push: routerPush }),
  useRoute: () => ({ params: {}, query: { patientId: '1' } })
}))

const mockedApi = api as unknown as {
  get: ReturnType<typeof vi.fn>
  post: ReturnType<typeof vi.fn>
}

const patientSummary = {
  id: 1,
  firstName: 'Juan',
  lastName: 'Perez',
  sex: 'MALE',
  hasActiveAdmission: false
}

const doctor = { id: 10, salutation: 'DR', firstName: 'Maria', lastName: 'Garcia', username: 'doctor' }
const resident = { id: 20, salutation: 'DRA', firstName: 'Andrea', lastName: 'Pineda', username: 'resident' }

function ok<T>(payload: T) {
  return { data: { success: true, data: payload } }
}

function setupApi() {
  mockedApi.get.mockImplementation((url: string) => {
    if (url === '/v1/admissions/residents') return Promise.resolve(ok([resident]))
    if (url === '/v1/admissions/doctors') return Promise.resolve(ok([doctor]))
    if (url.startsWith('/v1/admissions/patients/')) return Promise.resolve(ok(patientSummary))
    if (url === '/v1/triage-codes') return Promise.resolve(ok([]))
    if (url === '/v1/rooms/available') return Promise.resolve(ok([]))
    return Promise.resolve(ok([]))
  })
  mockedApi.post.mockResolvedValue(ok({ id: 99 }))
}

function mountForm(roles: string[]) {
  const i18n = createI18n({ legacy: false, locale: 'en', fallbackLocale: 'en', messages: { en, es } })
  return mount(AdmissionFormView, {
    global: {
      plugins: [
        createTestingPinia({
          stubActions: false,
          initialState: { auth: { user: { roles, permissions: [] } } }
        }),
        PrimeVue,
        i18n
      ],
      stubs: { ProgressSpinner: true }
    }
  })
}

describe('AdmissionFormView resident picker', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    setupApi()
  })

  it('shows the resident picker and fetches residents for an admin', async () => {
    const wrapper = mountForm(['ADMIN'])
    await flushPromises()

    expect(mockedApi.get).toHaveBeenCalledWith('/v1/admissions/residents')
    expect(wrapper.find('[data-testid="resident-field"]').exists()).toBe(true)
  })

  it('hides the resident picker and does not fetch residents for a resident doctor', async () => {
    const wrapper = mountForm(['RESIDENT_DOCTOR'])
    await flushPromises()

    expect(mockedApi.get).not.toHaveBeenCalledWith('/v1/admissions/residents')
    expect(wrapper.find('[data-testid="resident-field"]').exists()).toBe(false)
  })

  it('blocks registration for a role that may not admit', async () => {
    const wrapper = mountForm(['DOCTOR'])
    await flushPromises()

    expect(wrapper.find('[data-testid="resident-field"]').exists()).toBe(false)
    // Warning banner is shown when the user is neither admin nor resident.
    expect(wrapper.text()).toContain(en.admission.residentRoleRequired)
  })

  it('requires an admin to select a resident before submit', async () => {
    const wrapper = mountForm(['ADMIN'])
    await flushPromises()

    Object.assign(wrapper.vm, {
      selectedType: AdmissionType.AMBULATORY,
      selectedPhysician: doctor.id
    })

    await wrapper.vm.submitAdmission()
    await flushPromises()

    expect(mockedApi.post).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain(en.validation.admission.residentId.required)
  })

  it('sends residentId only for admin-created admissions', async () => {
    const adminWrapper = mountForm(['ADMIN'])
    await flushPromises()

    Object.assign(adminWrapper.vm, {
      selectedType: AdmissionType.AMBULATORY,
      selectedPhysician: doctor.id,
      selectedResident: resident.id
    })

    await adminWrapper.vm.submitAdmission()
    await flushPromises()

    expect(mockedApi.post).toHaveBeenCalledWith(
      '/v1/admissions',
      expect.objectContaining({
        patientId: patientSummary.id,
        roomId: null,
        triageCodeId: null,
        treatingPhysicianId: doctor.id,
        type: AdmissionType.AMBULATORY,
        residentId: resident.id
      })
    )

    mockedApi.post.mockClear()
    const residentWrapper = mountForm(['RESIDENT_DOCTOR'])
    await flushPromises()

    Object.assign(residentWrapper.vm, {
      selectedType: AdmissionType.AMBULATORY,
      selectedPhysician: doctor.id
    })

    await residentWrapper.vm.submitAdmission()
    await flushPromises()

    const payload = mockedApi.post.mock.calls[0]![1]
    expect(payload).toMatchObject({
      patientId: patientSummary.id,
      roomId: null,
      triageCodeId: null,
      treatingPhysicianId: doctor.id,
      type: AdmissionType.AMBULATORY
    })
    expect(payload).not.toHaveProperty('residentId')
  })
})
