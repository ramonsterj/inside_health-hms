import { describe, it, expect, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createI18n } from 'vue-i18n'
import AdmissionCard from './AdmissionCard.vue'
import en from '@/i18n/locales/en.json'
import es from '@/i18n/locales/es.json'
import { AdmissionStatus, AdmissionType, type AdmissionListItem } from '@/types/admission'
import { Sex } from '@/types/patient'
import { RoomGender, RoomType } from '@/types/room'

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
    triageCode: { id: 1, code: 'A', color: '#FF0000', description: 'Critical' },
    room: { id: 1, number: '204', type: RoomType.PRIVATE, gender: RoomGender.FEMALE },
    treatingPhysician: {
      id: 3,
      firstName: 'María',
      lastName: 'García',
      salutation: 'DR',
      username: 'maria.garcia'
    },
    admissionDate: new Date(Date.now() - 2 * 60 * 60 * 1000).toISOString(),
    dischargeDate: null,
    status: AdmissionStatus.ACTIVE,
    type: AdmissionType.HOSPITALIZATION,
    hasConsentDocument: false,
    createdAt: null,
    ...overrides
  }
}

function mountCard(
  props: { admission: AdmissionListItem; showStatus?: boolean },
  locale: 'en' | 'es' = 'en'
) {
  const i18n = createI18n({
    legacy: false,
    locale,
    fallbackLocale: 'en',
    messages: { en, es }
  })
  return mount(AdmissionCard, {
    props,
    global: {
      plugins: [i18n],
      stubs: {
        Tag: {
          props: ['value', 'severity'],
          template: '<span class="p-tag" :data-severity="severity">{{ value }}</span>'
        }
      }
    }
  })
}

describe('AdmissionCard', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('renders patient name and admission type label', () => {
    const wrapper = mountCard({ admission: buildAdmission() })
    expect(wrapper.text()).toContain('Juana Pérez')
    expect(wrapper.text()).toContain('Hospitalization')
  })

  it('renders the gender icon based on patient sex', () => {
    const female = mountCard({ admission: buildAdmission({ patient: buildAdmission().patient }) })
    expect(female.find('svg').exists()).toBe(true)

    const male = mountCard({
      admission: buildAdmission({
        patient: { ...buildAdmission().patient, sex: Sex.MALE }
      })
    })
    expect(male.find('svg').exists()).toBe(true)
  })

  it('renders the type as a colored pill with the type background class', () => {
    const wrapper = mountCard({
      admission: buildAdmission({ type: AdmissionType.EMERGENCY })
    })
    const pill = wrapper.find('.type-pill')
    expect(pill.exists()).toBe(true)
    expect(pill.classes()).toContain('bg-red-500')
    expect(pill.text()).toBe('Emergency')
  })

  it('omits the doctor row when treating physician name is missing', () => {
    const wrapper = mountCard({
      admission: buildAdmission({
        treatingPhysician: {
          id: 3,
          firstName: null,
          lastName: null,
          salutation: null,
          username: 'unknown'
        }
      })
    })
    expect(wrapper.text()).not.toContain('Doctor')
  })

  it('omits the room row when no room is assigned', () => {
    const wrapper = mountCard({ admission: buildAdmission({ room: null }) })
    expect(wrapper.text()).not.toContain('Room')
  })

  it('omits the triage row when no triage code is assigned', () => {
    const wrapper = mountCard({ admission: buildAdmission({ triageCode: null }) })
    expect(wrapper.text()).not.toContain('Triage')
  })

  it('renders the triage as a colored pill with the short description label', () => {
    const wrapper = mountCard({
      admission: buildAdmission({
        triageCode: {
          id: 1,
          code: 'A',
          color: '#FF0000',
          description: 'Critical - Immediate attention required'
        }
      })
    })
    const pill = wrapper.find('.triage-pill')
    expect(pill.exists()).toBe(true)
    expect(pill.text()).toBe('Critical')
    // The pill background is colored from the triage code.
    expect(pill.attributes('style')).toContain('background-color: #FF0000')
    // The code letter is no longer shown in the row.
    const triageRow = wrapper.find('.triage-cell')
    expect(triageRow.text()).not.toMatch(/^A\b/)
  })

  it('localizes the triage label via i18n when the locale is Spanish', () => {
    const wrapper = mountCard(
      {
        admission: buildAdmission({
          triageCode: {
            id: 1,
            code: 'A',
            color: '#FF0000',
            description: 'Critical - Immediate attention required'
          }
        })
      },
      'es'
    )
    // Spanish locale provides "Crítico - Atención inmediata requerida".
    expect(wrapper.find('.triage-pill').text()).toBe('Crítico')
  })

  it('falls back to the raw description for triage codes not in the i18n bundle', () => {
    const wrapper = mountCard({
      admission: buildAdmission({
        // Code 'Z' is admin-created and has no entry in triageCode.codes.
        triageCode: { id: 99, code: 'Z', color: '#777777', description: 'Walk-in' }
      })
    })
    expect(wrapper.find('.triage-pill').text()).toBe('Walk-in')
  })

  it('falls back to the code letter when no description is present', () => {
    const wrapper = mountCard({
      admission: buildAdmission({
        triageCode: { id: 99, code: 'Z', color: '#777777', description: null }
      })
    })
    expect(wrapper.find('.triage-pill').text()).toBe('Z')
  })

  it('renders the status row only when showStatus is set', () => {
    const without = mountCard({ admission: buildAdmission() })
    expect(without.text()).not.toContain('Status')

    const withStatus = mountCard({ admission: buildAdmission(), showStatus: true })
    expect(withStatus.text()).toContain('Status')
    expect(withStatus.text()).toContain('Active')
  })

  it('exposes the card as a button with an aria-label naming the patient', () => {
    const wrapper = mountCard({ admission: buildAdmission() })
    const card = wrapper.find('.admission-card')
    expect(card.attributes('role')).toBe('button')
    expect(card.attributes('tabindex')).toBe('0')
    expect(card.attributes('aria-label')).toContain('Juana Pérez')
  })

  it('emits the view event when the card is clicked anywhere', async () => {
    const wrapper = mountCard({ admission: buildAdmission({ id: 42 }) })
    await wrapper.find('.admission-card').trigger('click')
    expect(wrapper.emitted('view')).toEqual([[42]])
  })

  it('emits the view event when Enter or Space is pressed on the card', async () => {
    const wrapper = mountCard({ admission: buildAdmission({ id: 42 }) })
    await wrapper.find('.admission-card').trigger('keydown.enter')
    await wrapper.find('.admission-card').trigger('keydown.space')
    expect(wrapper.emitted('view')).toEqual([[42], [42]])
  })
})
