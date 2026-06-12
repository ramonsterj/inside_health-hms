import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import PrimeVue from 'primevue/config'
import en from '@/i18n/locales/en.json'
import es from '@/i18n/locales/es.json'
import FileViewerDialog from './FileViewerDialog.vue'

const { showError } = vi.hoisted(() => ({ showError: vi.fn() }))

vi.mock('@/composables/useErrorHandler', () => ({
  useErrorHandler: () => ({ showError })
}))

function mountDialog(options: { contentType?: string | null; fetchBlob: () => Promise<Blob> }) {
  const i18n = createI18n({
    legacy: false,
    locale: 'en',
    fallbackLocale: 'en',
    messages: { en, es }
  })
  return mount(FileViewerDialog, {
    props: {
      visible: true,
      title: 'Lab result',
      contentType: options.contentType,
      fetchBlob: options.fetchBlob
    },
    global: {
      plugins: [i18n, PrimeVue],
      stubs: {
        teleport: true,
        PdfViewerPane: true
      }
    }
  })
}

describe('FileViewerDialog', () => {
  let urlCounter = 0

  beforeEach(() => {
    urlCounter = 0
    URL.createObjectURL = vi.fn(() => `blob:mock-${++urlCounter}`)
    URL.revokeObjectURL = vi.fn()
  })

  it('renders an image document inline', async () => {
    const wrapper = mountDialog({
      contentType: 'image/png',
      fetchBlob: () => Promise.resolve(new Blob(['img'], { type: 'image/png' }))
    })
    await flushPromises()

    const img = wrapper.get('img')
    expect(img.attributes('src')).toBe('blob:mock-1')
    expect(img.attributes('alt')).toBe('Lab result')
  })

  it('renders a PDF document in the in-app pane (no new tab)', async () => {
    const openSpy = vi.fn()
    window.open = openSpy

    const wrapper = mountDialog({
      contentType: 'application/pdf',
      fetchBlob: () => Promise.resolve(new Blob(['%PDF'], { type: 'application/pdf' }))
    })
    await flushPromises()

    expect(wrapper.find('pdf-viewer-pane-stub').exists()).toBe(true)
    expect(openSpy).not.toHaveBeenCalled()
  })

  it('falls back to the blob type when no content type is provided', async () => {
    const wrapper = mountDialog({
      contentType: null,
      fetchBlob: () => Promise.resolve(new Blob(['%PDF'], { type: 'application/pdf' }))
    })
    await flushPromises()

    expect(wrapper.find('pdf-viewer-pane-stub').exists()).toBe(true)
  })

  it('shows the unsupported-type fallback with a download action', async () => {
    const wrapper = mountDialog({
      contentType: 'text/plain',
      fetchBlob: () => Promise.resolve(new Blob(['hello'], { type: 'text/plain' }))
    })
    await flushPromises()

    expect(wrapper.text()).toContain('This file type cannot be previewed')
  })

  it('surfaces fetch errors and closes the dialog', async () => {
    const wrapper = mountDialog({
      contentType: 'application/pdf',
      fetchBlob: () => Promise.reject(new Error('forbidden'))
    })
    await flushPromises()

    expect(showError).toHaveBeenCalled()
    expect(wrapper.emitted('update:visible')?.at(-1)).toEqual([false])
  })

  it('revokes the image object URL when the dialog closes', async () => {
    const wrapper = mountDialog({
      contentType: 'image/jpeg',
      fetchBlob: () => Promise.resolve(new Blob(['img'], { type: 'image/jpeg' }))
    })
    await flushPromises()
    expect(URL.createObjectURL).toHaveBeenCalledTimes(1)

    await wrapper.setProps({ visible: false })

    expect(URL.revokeObjectURL).toHaveBeenCalledWith('blob:mock-1')
  })
})
