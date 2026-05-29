import { describe, it, expect, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia, type Pinia } from 'pinia'
import { createI18n } from 'vue-i18n'
import PrimeVue from 'primevue/config'
import Button from 'primevue/button'
import KardexMedicationList from './KardexMedicationList.vue'
import en from '@/i18n/locales/en.json'
import es from '@/i18n/locales/es.json'
import { useAuthStore } from '@/stores/auth'
import type { KardexMedicationSummary } from '@/types'

const sampleMedication: KardexMedicationSummary = {
  orderId: 1,
  medication: 'Lorazepam',
  dosage: '1mg',
  route: 'ORAL',
  frequency: 'Every 8 hours',
  schedule: null,
  inventoryItemId: null,
  inventoryItemName: null,
  nextLotExpirationDate: null,
  lastAdministration: null
} as unknown as KardexMedicationSummary

function mountList(roles: string[], permissions: string[]) {
  const pinia: Pinia = createPinia()
  setActivePinia(pinia)
  const auth = useAuthStore()
  auth.$patch({ user: { roles, permissions } } as never)

  const i18n = createI18n({
    legacy: false,
    locale: 'en',
    fallbackLocale: 'en',
    messages: { en, es }
  })

  return mount(KardexMedicationList, {
    props: { medications: [sampleMedication], admissionId: 1 },
    global: {
      plugins: [pinia, PrimeVue, i18n],
      stubs: {
        MedicationAdministrationDialog: true,
        ExpiryStatusChip: true
      }
    }
  })
}

describe('KardexMedicationList — Administer button gating', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('hides the Administer button for an auxiliary nurse (no medication-administration:create)', () => {
    const wrapper = mountList(['AUXILIARY_NURSE'], [
      'vital-sign:create',
      'nursing-note:create',
      'medication-administration:read'
    ])

    expect(wrapper.findAllComponents(Button)).toHaveLength(0)
  })

  it('hides the Administer button for an auxiliary-only nurse even when a custom role grants the permission', () => {
    // Models the AC-3 scenario: a custom role accidentally grants medication-administration:create
    // to an AUXILIARY_NURSE-only user. The button must still hide because the backend 403s.
    const wrapper = mountList(['AUXILIARY_NURSE'], [
      'medication-administration:create',
      'medication-administration:read'
    ])

    expect(wrapper.findAllComponents(Button)).toHaveLength(0)
  })

  it('shows the Administer button for a nurse stacked with auxiliary role', () => {
    // A graduate nurse covering an auxiliary shift keeps NURSE, so the restriction does not apply.
    const wrapper = mountList(['NURSE', 'AUXILIARY_NURSE'], ['medication-administration:create'])

    expect(wrapper.findAllComponents(Button)).toHaveLength(1)
  })

  it('shows the Administer button for a graduate nurse with medication-administration:create', () => {
    const wrapper = mountList(['NURSE'], [
      'medication-administration:create',
      'medication-administration:read'
    ])

    expect(wrapper.findAllComponents(Button)).toHaveLength(1)
  })

  it('shows the Administer button for an admin (all permissions implied)', () => {
    const wrapper = mountList(['ADMIN'], [])

    expect(wrapper.findAllComponents(Button)).toHaveLength(1)
  })
})
