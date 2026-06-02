import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import PrimeVue from 'primevue/config'
import { createI18n } from 'vue-i18n'
import en from '@/i18n/locales/en.json'
import es from '@/i18n/locales/es.json'
import AdmissionHeroHeader from './AdmissionHeroHeader.vue'
import { AdmissionStatus, AdmissionType, type AdmissionDetail } from '@/types/admission'
import { Sex } from '@/types/patient'
import { RoomType, RoomGender } from '@/types/room'

function i18n() {
  return createI18n({ legacy: false, locale: 'en', fallbackLocale: 'en', messages: { en, es } })
}

function makeAdmission(overrides: Partial<AdmissionDetail> = {}): AdmissionDetail {
  return {
    id: 1,
    patient: {
      id: 5,
      firstName: 'Maria',
      lastName: 'Santos',
      dateOfBirth: '1990-01-01',
      age: 36,
      sex: Sex.FEMALE,
      email: 'm@example.com',
      idDocumentNumber: 'DPI-12345',
      hasIdDocument: true,
      hasActiveAdmission: true
    },
    triageCode: { id: 1, code: 'A', color: '#ef4444', description: null },
    room: { id: 2, number: '101', type: RoomType.PRIVATE, gender: RoomGender.FEMALE },
    treatingPhysician: {
      id: 3,
      salutation: 'DRA',
      firstName: 'Claudia',
      lastName: 'Barrios',
      username: 'cbarrios'
    },
    resident: {
      id: 4,
      salutation: 'DR',
      firstName: 'Andrea',
      lastName: 'Pineda',
      username: 'apineda'
    },
    admissionDate: '2026-05-20T14:15:00',
    dischargeDate: null,
    status: AdmissionStatus.ACTIVE,
    type: AdmissionType.HOSPITALIZATION,
    inventory: null,
    hasConsentDocument: false,
    consultingPhysicians: [],
    createdAt: null,
    createdBy: null,
    updatedAt: null,
    updatedBy: null,
    ...overrides
  } as AdmissionDetail
}

function mountHero(admission: AdmissionDetail, status: AdmissionStatus) {
  return mount(AdmissionHeroHeader, {
    props: { admission, admissionStatus: status },
    global: { plugins: [PrimeVue, i18n()] }
  })
}

describe('AdmissionHeroHeader', () => {
  it('renders name, document, room, physicians and admission date', () => {
    const wrapper = mountHero(makeAdmission(), AdmissionStatus.ACTIVE)
    const text = wrapper.text()
    expect(text).toContain('Maria Santos')
    expect(text).toContain('DPI-12345')
    expect(text).toContain('101')
    expect(text).toContain('Claudia Barrios')
    expect(text).toContain('Andrea Pineda')
    // Admission date via formatDateTime (dd/MM/yyyy - HH:mm).
    expect(text).toContain('20/05/2026 - 14:15')
  })

  it('renders avatar initials from the patient name', () => {
    const wrapper = mountHero(makeAdmission(), AdmissionStatus.ACTIVE)
    expect(wrapper.find('.hero-avatar').text()).toBe('MS')
  })

  it('applies a readable contrast color to the triage chip', () => {
    const wrapper = mountHero(makeAdmission(), AdmissionStatus.ACTIVE)
    const triage = wrapper.find('.chip-triage')
    expect(triage.exists()).toBe(true)
    // #ef4444 is a mid/dark red → white text (getContrastColor returns #FFFFFF).
    expect(triage.attributes('style')).toContain('color: #FFFFFF')
  })

  it('shows the discharge date + chip and the muted gradient class when discharged', () => {
    const wrapper = mountHero(
      makeAdmission({ status: AdmissionStatus.DISCHARGED, dischargeDate: '2026-06-01T10:00:00' }),
      AdmissionStatus.DISCHARGED
    )
    expect(wrapper.find('.patient-hero').classes()).toContain('is-discharged')
    expect(wrapper.text()).toContain('01/06/2026 - 10:00')
    expect(wrapper.find('.chip-active').exists()).toBe(false)
  })

  it('hides the discharge fact while active', () => {
    const wrapper = mountHero(makeAdmission(), AdmissionStatus.ACTIVE)
    expect(wrapper.find('.patient-hero').classes()).not.toContain('is-discharged')
    expect(wrapper.find('.chip-active').exists()).toBe(true)
  })
})
