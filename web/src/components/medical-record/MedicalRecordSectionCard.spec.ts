import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import PrimeVue from 'primevue/config'
import { createI18n } from 'vue-i18n'
import en from '@/i18n/locales/en.json'
import es from '@/i18n/locales/es.json'
import MedicalRecordSectionCard from './MedicalRecordSectionCard.vue'

function i18n() {
  return createI18n({ legacy: false, locale: 'en', fallbackLocale: 'en', messages: { en, es } })
}

function mountCard(props: Record<string, unknown>) {
  return mount(MedicalRecordSectionCard, {
    props: {
      sectionKey: 'clinicalHistory',
      title: 'Clinical History',
      icon: 'pi pi-book',
      ...props
    },
    global: { plugins: [PrimeVue, i18n()] }
  })
}

describe('MedicalRecordSectionCard', () => {
  it('exposes a stable section testid for e2e targeting', () => {
    const wrapper = mountCard({ sectionKey: 'documents' })
    expect(wrapper.find('[data-testid="section-card-documents"]').exists()).toBe(true)
  })

  it('renders the title and metric tag when a metric is provided', () => {
    const wrapper = mountCard({ metric: '6 notes', metricSeverity: 'info' })
    expect(wrapper.text()).toContain('Clinical History')
    expect(wrapper.text()).toContain('6 notes')
  })

  it('renders nothing for the metric when undefined (graceful degradation)', () => {
    const wrapper = mountCard({})
    expect(wrapper.find('.p-tag').exists()).toBe(false)
  })

  it('shows the updated line only when provided', () => {
    const without = mountCard({})
    expect(without.find('.section-updated').exists()).toBe(false)

    const withUpdated = mountCard({ updated: '09/05/2026 - 14:30' })
    expect(withUpdated.find('.section-updated').text()).toContain('09/05/2026 - 14:30')
  })

  it('emits open on click', async () => {
    const wrapper = mountCard({})
    await wrapper.find('[data-testid="section-card-clinicalHistory"]').trigger('click')
    expect(wrapper.emitted('open')).toHaveLength(1)
  })
})
