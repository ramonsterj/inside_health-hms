import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import MedicalOrderStateBadge from './MedicalOrderStateBadge.vue'
import en from '@/i18n/locales/en.json'
import es from '@/i18n/locales/es.json'
import { MedicalOrderStatus } from '@/types/medicalRecord'

function mountBadge(status: MedicalOrderStatus, locale: 'en' | 'es' = 'en') {
  const i18n = createI18n({
    legacy: false,
    locale,
    fallbackLocale: 'en',
    messages: { en, es }
  })
  return mount(MedicalOrderStateBadge, {
    props: { status },
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

describe('MedicalOrderStateBadge', () => {
  it('renders the localized label for SOLICITADO', () => {
    const wrapper = mountBadge(MedicalOrderStatus.SOLICITADO)
    expect(wrapper.text()).toBe('Requested')
    expect(wrapper.find('.p-tag').attributes('data-severity')).toBe('info')
  })

  it('renders the localized label for AUTORIZADO with success severity', () => {
    const wrapper = mountBadge(MedicalOrderStatus.AUTORIZADO)
    expect(wrapper.text()).toBe('Authorized')
    expect(wrapper.find('.p-tag').attributes('data-severity')).toBe('success')
  })

  it('renders NO_AUTORIZADO with danger severity', () => {
    const wrapper = mountBadge(MedicalOrderStatus.NO_AUTORIZADO)
    expect(wrapper.find('.p-tag').attributes('data-severity')).toBe('danger')
  })

  it('renders ACTIVA with success severity', () => {
    const wrapper = mountBadge(MedicalOrderStatus.ACTIVA)
    expect(wrapper.find('.p-tag').attributes('data-severity')).toBe('success')
  })

  it('renders EN_PROCESO with warn severity', () => {
    const wrapper = mountBadge(MedicalOrderStatus.EN_PROCESO)
    expect(wrapper.find('.p-tag').attributes('data-severity')).toBe('warn')
  })

  it('renders RESULTADOS_RECIBIDOS with contrast severity', () => {
    const wrapper = mountBadge(MedicalOrderStatus.RESULTADOS_RECIBIDOS)
    expect(wrapper.find('.p-tag').attributes('data-severity')).toBe('contrast')
  })

  it('renders DESCONTINUADO with secondary severity', () => {
    const wrapper = mountBadge(MedicalOrderStatus.DESCONTINUADO)
    expect(wrapper.find('.p-tag').attributes('data-severity')).toBe('secondary')
  })

  it('localizes to Spanish', () => {
    const wrapper = mountBadge(MedicalOrderStatus.SOLICITADO, 'es')
    expect(wrapper.text()).toBe('Solicitado')
  })
})
