import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import PrimeVue from 'primevue/config'
import en from '@/i18n/locales/en.json'
import es from '@/i18n/locales/es.json'
import SearchInput from './SearchInput.vue'

function i18n() {
  return createI18n({ legacy: false, locale: 'en', fallbackLocale: 'en', messages: { en, es } })
}

function mountSearch(props: Record<string, unknown> = {}) {
  return mount(SearchInput, {
    props,
    global: { plugins: [PrimeVue, i18n()] }
  })
}

describe('SearchInput', () => {
  beforeEach(() => {
    vi.useFakeTimers()
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  async function typeInto(wrapper: ReturnType<typeof mountSearch>, value: string) {
    const input = wrapper.find('input')
    await input.setValue(value)
  }

  it('does not emit search below 3 characters', async () => {
    const wrapper = mountSearch()
    await typeInto(wrapper, 'ab')
    vi.advanceTimersByTime(500)
    expect(wrapper.emitted('search')).toBeUndefined()
  })

  it('emits the committed term (debounced) at 3+ characters', async () => {
    const wrapper = mountSearch()
    await typeInto(wrapper, 'abc')
    expect(wrapper.emitted('search')).toBeUndefined()
    vi.advanceTimersByTime(300)
    expect(wrapper.emitted('search')).toEqual([['abc']])
  })

  it('shows the min-chars hint only for partial input', async () => {
    const wrapper = mountSearch()
    expect(wrapper.find('.search-input-hint').exists()).toBe(false)

    await typeInto(wrapper, 'ab')
    expect(wrapper.find('.search-input-hint').exists()).toBe(true)
    expect(wrapper.find('.search-input-hint').text()).toContain('3')

    await typeInto(wrapper, 'abc')
    expect(wrapper.find('.search-input-hint').exists()).toBe(false)
  })

  it('emits an empty term when cleared', async () => {
    const wrapper = mountSearch()
    await typeInto(wrapper, 'abc')
    vi.advanceTimersByTime(300)
    await typeInto(wrapper, '')
    vi.advanceTimersByTime(300)
    expect(wrapper.emitted('search')).toEqual([['abc'], ['']])
  })

  it('respects a custom minLength', async () => {
    const wrapper = mountSearch({ minLength: 1 })
    await typeInto(wrapper, 'a')
    vi.advanceTimersByTime(300)
    expect(wrapper.emitted('search')).toEqual([['a']])
  })

  it('reset() clears the box without emitting', async () => {
    const wrapper = mountSearch()
    await typeInto(wrapper, 'abc')
    vi.advanceTimersByTime(300)
    ;(wrapper.vm as unknown as { reset: () => void }).reset()
    await wrapper.vm.$nextTick()
    vi.advanceTimersByTime(300)
    expect(wrapper.find('input').element.value).toBe('')
    // Only the initial 'abc' emit, no empty emit from reset.
    expect(wrapper.emitted('search')).toEqual([['abc']])
  })
})
