import { describe, it, expect, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import en from '@/i18n/locales/en.json'
import es from '@/i18n/locales/es.json'
import PdfViewerPane from './PdfViewerPane.vue'

const { renderCancel, fakePage, fakeDoc, destroyLoadingTask, getDocument } = vi.hoisted(() => {
  const renderCancel = vi.fn()
  const fakePage = {
    getViewport: ({ scale }: { scale: number }) => ({ width: 600 * scale, height: 800 * scale }),
    render: vi.fn(() => ({ promise: Promise.resolve(), cancel: renderCancel }))
  }
  const fakeDoc = {
    numPages: 3,
    getPage: vi.fn(() => Promise.resolve(fakePage))
  }
  const destroyLoadingTask = vi.fn(() => Promise.resolve())
  const getDocument = vi.fn(() => ({
    promise: Promise.resolve(fakeDoc),
    destroy: destroyLoadingTask
  }))
  return { renderCancel, fakePage, fakeDoc, destroyLoadingTask, getDocument }
})

vi.mock('./pdfjsLoader', () => ({
  loadPdfjs: () => Promise.resolve({ getDocument })
}))

function makePdfBlob(): Blob {
  const blob = new Blob(['%PDF-1.7'], { type: 'application/pdf' })
  blob.arrayBuffer = () => Promise.resolve(new ArrayBuffer(8))
  return blob
}

async function mountPane() {
  const i18n = createI18n({
    legacy: false,
    locale: 'en',
    fallbackLocale: 'en',
    messages: { en, es }
  })
  const wrapper = mount(PdfViewerPane, {
    props: { blob: makePdfBlob() },
    global: { plugins: [i18n] }
  })
  await flushPromises()
  await flushPromises()
  return wrapper
}

function button(wrapper: Awaited<ReturnType<typeof mountPane>>, ariaLabel: string) {
  const match = wrapper.findAll('button').find(b => b.attributes('aria-label') === ariaLabel)
  if (!match) throw new Error(`button "${ariaLabel}" not found`)
  return match
}

describe('PdfViewerPane', () => {
  it('loads the document and renders page 1 of N', async () => {
    const wrapper = await mountPane()

    expect(getDocument).toHaveBeenCalledTimes(1)
    expect(fakeDoc.getPage).toHaveBeenCalledWith(1)
    expect(wrapper.get('[data-testid="pdf-page-indicator"]').text()).toBe('Page 1 of 3')
    expect(fakePage.render).toHaveBeenCalled()
  })

  it('navigates pages with the buttons and disables them at the bounds', async () => {
    const wrapper = await mountPane()

    expect(button(wrapper, 'Previous page').attributes('disabled')).toBeDefined()

    await button(wrapper, 'Next page').trigger('click')
    await flushPromises()
    expect(fakeDoc.getPage).toHaveBeenCalledWith(2)
    expect(wrapper.get('[data-testid="pdf-page-indicator"]').text()).toBe('Page 2 of 3')

    await button(wrapper, 'Next page').trigger('click')
    await flushPromises()
    expect(wrapper.get('[data-testid="pdf-page-indicator"]').text()).toBe('Page 3 of 3')
    expect(button(wrapper, 'Next page').attributes('disabled')).toBeDefined()
  })

  it('navigates pages with the arrow keys', async () => {
    const wrapper = await mountPane()

    window.dispatchEvent(new KeyboardEvent('keydown', { key: 'ArrowRight' }))
    await flushPromises()
    expect(wrapper.get('[data-testid="pdf-page-indicator"]').text()).toBe('Page 2 of 3')

    window.dispatchEvent(new KeyboardEvent('keydown', { key: 'ArrowLeft' }))
    await flushPromises()
    expect(wrapper.get('[data-testid="pdf-page-indicator"]').text()).toBe('Page 1 of 3')

    wrapper.unmount()
  })

  it('re-renders on zoom changes', async () => {
    const wrapper = await mountPane()
    const rendersBefore = fakePage.render.mock.calls.length

    await button(wrapper, 'Zoom in').trigger('click')
    await flushPromises()

    expect(fakePage.render.mock.calls.length).toBe(rendersBefore + 1)
    expect(wrapper.text()).toContain('125%')
  })

  it('cancels the render task and destroys the document on unmount', async () => {
    const wrapper = await mountPane()

    wrapper.unmount()

    expect(renderCancel).toHaveBeenCalled()
    expect(destroyLoadingTask).toHaveBeenCalled()
  })

  it('shows the render error state when the document cannot be parsed', async () => {
    getDocument.mockReturnValueOnce({
      promise: Promise.reject(new Error('Invalid PDF structure')),
      destroy: destroyLoadingTask
    } as never)

    const wrapper = await mountPane()

    expect(wrapper.text()).toContain('The document could not be displayed')
  })
})
