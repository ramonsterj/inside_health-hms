import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createI18n } from 'vue-i18n'
import PrimeVue from 'primevue/config'
import Button from 'primevue/button'
import ToastService from 'primevue/toastservice'
import { AxiosError, AxiosHeaders } from 'axios'
import AdmissionExportButton from './AdmissionExportButton.vue'
import en from '@/i18n/locales/en.json'
import es from '@/i18n/locales/es.json'
import { useAuthStore } from '@/stores/auth'
import api from '@/services/api'

const showErrorMock = vi.hoisted(() => vi.fn())

vi.mock('@/services/api', () => ({
  default: {
    get: vi.fn()
  }
}))

vi.mock('@/composables/useErrorHandler', () => ({
  useErrorHandler: () => ({
    showError: showErrorMock
  })
}))

function mountButton(locale: 'en' | 'es' = 'en') {
  const i18n = createI18n({
    legacy: false,
    locale,
    fallbackLocale: 'en',
    messages: { en, es }
  })
  return mount(AdmissionExportButton, {
    props: { admissionId: 42 },
    global: {
      plugins: [createPinia(), PrimeVue, ToastService, i18n]
    }
  })
}

describe('AdmissionExportButton', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
    // jsdom doesn't implement URL.createObjectURL
    if (!window.URL.createObjectURL) {
      window.URL.createObjectURL = vi.fn(() => 'blob:mock')
      window.URL.revokeObjectURL = vi.fn()
    } else {
      vi.spyOn(window.URL, 'createObjectURL').mockReturnValue('blob:mock')
      vi.spyOn(window.URL, 'revokeObjectURL').mockImplementation(() => {})
    }
  })

  it('is hidden when the user lacks the permission', () => {
    const wrapper = mountButton()
    const auth = useAuthStore()
    auth.$patch({ user: { roles: ['DOCTOR'], permissions: [] } } as never)

    expect(wrapper.find('[data-testid="admission-export-button"]').exists()).toBe(false)
  })

  it('is visible when the user has the permission', async () => {
    const wrapper = mountButton()
    const auth = useAuthStore()
    auth.$patch({ user: { roles: ['ADMINISTRATIVE_STAFF'], permissions: ['admission:export-pdf'] } } as never)
    await wrapper.vm.$nextTick()

    expect(wrapper.find('[data-testid="admission-export-button"]').exists()).toBe(true)
  })

  it('calls the export endpoint with responseType blob on click', async () => {
    const wrapper = mountButton()
    const auth = useAuthStore()
    auth.$patch({ user: { roles: ['ADMIN'], permissions: [] } } as never)
    await wrapper.vm.$nextTick()

    ;(api.get as ReturnType<typeof vi.fn>).mockResolvedValue({
      data: new Blob(['%PDF-1.4']),
      headers: { 'content-disposition': 'attachment; filename="admission-42-doe-20260511-1400.pdf"' }
    })

    await wrapper.find('[data-testid="admission-export-button"]').trigger('click')
    await flushPromises()

    expect(api.get).toHaveBeenCalledWith(
      '/v1/admissions/42/export.pdf',
      expect.objectContaining({ responseType: 'blob' })
    )
  })

  it('toggles loading around the download call', async () => {
    const wrapper = mountButton()
    const auth = useAuthStore()
    auth.$patch({ user: { roles: ['ADMIN'], permissions: [] } } as never)
    await wrapper.vm.$nextTick()

    let resolveRequest!: (value: unknown) => void
    ;(api.get as ReturnType<typeof vi.fn>).mockReturnValue(
      new Promise((resolve) => {
        resolveRequest = resolve
      })
    )

    await wrapper.find('[data-testid="admission-export-button"]').trigger('click')
    await wrapper.vm.$nextTick()
    expect(wrapper.findComponent(Button).props('loading')).toBe(true)

    resolveRequest({ data: new Blob(['%PDF-1.4']), headers: {} })
    await flushPromises()

    expect(wrapper.findComponent(Button).props('loading')).toBe(false)
  })

  it('passes rejected downloads to the shared error handler', async () => {
    const wrapper = mountButton()
    const auth = useAuthStore()
    auth.$patch({ user: { roles: ['ADMIN'], permissions: [] } } as never)
    await wrapper.vm.$nextTick()
    const error = new Error('too large')
    ;(api.get as ReturnType<typeof vi.fn>).mockRejectedValue(error)

    await wrapper.find('[data-testid="admission-export-button"]').trigger('click')
    await flushPromises()

    expect(showErrorMock).toHaveBeenCalledWith(error)
  })

  it('decodes blob error bodies before delegating to the error handler', async () => {
    const wrapper = mountButton()
    const auth = useAuthStore()
    auth.$patch({ user: { roles: ['ADMIN'], permissions: [] } } as never)
    await wrapper.vm.$nextTick()

    const payload = { error: { code: 'PAYLOAD_TOO_LARGE', message: 'too large' } }
    const blob = new Blob([JSON.stringify(payload)], { type: 'application/json' })
    const axiosError = new AxiosError(
      'Request failed with status code 413',
      'ERR_BAD_REQUEST',
      undefined,
      undefined,
      {
        data: blob,
        status: 413,
        statusText: 'Payload Too Large',
        headers: {},
        config: { headers: new AxiosHeaders() }
      }
    )
    ;(api.get as ReturnType<typeof vi.fn>).mockRejectedValue(axiosError)

    await wrapper.find('[data-testid="admission-export-button"]').trigger('click')
    await flushPromises()

    expect(showErrorMock).toHaveBeenCalledTimes(1)
    const handed = showErrorMock.mock.calls[0]![0] as AxiosError
    expect(handed.response?.data).toEqual(payload)
  })
})
