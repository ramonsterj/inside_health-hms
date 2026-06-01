import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createRouter, createMemoryHistory, type Router } from 'vue-router'
import { createI18n } from 'vue-i18n'
import PrimeVue from 'primevue/config'
import PatientAdmissionsHistory from './PatientAdmissionsHistory.vue'
import en from '@/i18n/locales/en.json'
import es from '@/i18n/locales/es.json'
import { useAuthStore } from '@/stores/auth'
import { useAdmissionStore } from '@/stores/admission'
import { AdmissionStatus, AdmissionType, type AdmissionListItem } from '@/types/admission'
import { Sex } from '@/types/patient'
import { RoomGender, RoomType } from '@/types/room'

// Avoid pulling in PrimeVue's ToastService (useToast) in the composable under test.
vi.mock('@/composables/useErrorHandler', () => ({
  useErrorHandler: () => ({ showError: vi.fn() })
}))

function buildAdmission(overrides: Partial<AdmissionListItem> = {}): AdmissionListItem {
  return {
    id: 1,
    patient: {
      id: 10,
      firstName: 'Juana',
      lastName: 'Pérez',
      dateOfBirth: '1991-01-01',
      age: 35,
      sex: Sex.FEMALE,
      email: 'juana@example.com',
      idDocumentNumber: null,
      hasIdDocument: false,
      hasActiveAdmission: true
    },
    triageCode: null,
    room: { id: 1, number: '204', type: RoomType.PRIVATE, gender: RoomGender.FEMALE },
    treatingPhysician: {
      id: 3,
      firstName: 'María',
      lastName: 'García',
      salutation: 'DR',
      username: 'maria.garcia'
    },
    resident: {
      id: 4,
      firstName: 'Carlos',
      lastName: 'López',
      salutation: 'DR',
      username: 'carlos.lopez'
    },
    admissionDate: '2026-05-20T14:30:00',
    dischargeDate: '2026-05-28T09:00:00',
    status: AdmissionStatus.DISCHARGED,
    type: AdmissionType.HOSPITALIZATION,
    hasConsentDocument: false,
    createdAt: null,
    ...overrides
  }
}

async function mountHistory(options: {
  admissions: AdmissionListItem[]
  total?: number
  permissions?: string[]
}) {
  setActivePinia(createPinia())

  const auth = useAuthStore()
  auth.$patch({
    user: { roles: ['NURSE'], permissions: options.permissions ?? ['patient:read'] }
  } as never)

  const store = useAdmissionStore()
  store.patientAdmissions = options.admissions
  store.totalPatientAdmissions = options.total ?? options.admissions.length
  // Keep onMounted from hitting the real API; state is seeded above.
  vi.spyOn(store, 'fetchPatientAdmissions').mockResolvedValue(undefined)

  const i18n = createI18n({
    legacy: false,
    locale: 'en',
    fallbackLocale: 'en',
    messages: { en, es }
  })
  const router: Router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/', component: { template: '<div />' } },
      { path: '/admissions/:id', name: 'admission-detail', component: { template: '<div />' } },
      { path: '/patients/:id', name: 'patient-detail', component: { template: '<div />' } }
    ]
  })

  const wrapper = mount(PatientAdmissionsHistory, {
    props: { patientId: 10 },
    global: { plugins: [PrimeVue, i18n, router] }
  })
  await flushPromises()
  return { wrapper, router }
}

describe('PatientAdmissionsHistory', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('renders a row per admission with date, type, room and physician', async () => {
    const { wrapper } = await mountHistory({ admissions: [buildAdmission()] })

    const rows = wrapper.findAll('tbody tr')
    expect(rows).toHaveLength(1)
    const text = wrapper.text()
    expect(text).toContain('Hospitalization')
    expect(text).toContain('204')
    expect(text).toContain('María García')
    // Datetime via formatDateTime (dd/MM/yyyy - HH:mm).
    expect(text).toContain('20/05/2026 - 14:30')
  })

  it('shows an "Active" badge when the admission has no discharge date', async () => {
    const { wrapper } = await mountHistory({
      admissions: [buildAdmission({ dischargeDate: null, status: AdmissionStatus.ACTIVE })]
    })
    expect(wrapper.text()).toContain('Active')
  })

  it('shows the empty state when there are no admissions', async () => {
    const { wrapper } = await mountHistory({ admissions: [] })
    expect(wrapper.text()).toContain('No admissions recorded')
    expect(wrapper.find('tbody tr').exists()).toBe(false)
  })

  it('navigates to the admission detail when the user holds admission:read', async () => {
    const { wrapper, router } = await mountHistory({
      admissions: [buildAdmission({ id: 99 })],
      permissions: ['patient:read', 'admission:read']
    })
    const push = vi.spyOn(router, 'push')

    await wrapper.find('tbody tr').trigger('click')

    expect(push).toHaveBeenCalledWith({ name: 'admission-detail', params: { id: 99 } })
  })

  it('does not navigate when the user lacks admission:read', async () => {
    const { wrapper, router } = await mountHistory({
      admissions: [buildAdmission({ id: 99 })],
      permissions: ['patient:read']
    })
    const push = vi.spyOn(router, 'push')

    await wrapper.find('tbody tr').trigger('click')

    expect(push).not.toHaveBeenCalled()
  })
})
