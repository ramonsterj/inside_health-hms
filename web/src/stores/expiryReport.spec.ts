import { describe, it, expect, beforeEach, vi, afterEach, type Mock } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useExpiryReportStore } from './expiryReport'
import api from '@/services/api'

vi.mock('@/services/api', () => ({
  default: {
    get: vi.fn()
  }
}))

const mockedApi = api as unknown as { get: Mock }

describe('useExpiryReportStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
    mockedApi.get.mockResolvedValue({
      data: { success: true, data: { generatedAt: '', totals: {}, items: [] } }
    })
  })

  afterEach(() => {
    vi.resetAllMocks()
  })

  it('omits warehouseId when not provided (system-wide aggregate)', async () => {
    const store = useExpiryReportStore()
    await store.fetch({ window: 90 })

    expect(mockedApi.get).toHaveBeenCalledWith('/v1/medications/expiry-report', {
      params: { window: 90 }
    })
  })

  it('passes warehouseId through to the API when provided (FR-8 warehouse facet)', async () => {
    const store = useExpiryReportStore()
    await store.fetch({ window: 90, warehouseId: 2 })

    expect(mockedApi.get).toHaveBeenCalledWith('/v1/medications/expiry-report', {
      params: { window: 90, warehouseId: 2 }
    })
  })
})
