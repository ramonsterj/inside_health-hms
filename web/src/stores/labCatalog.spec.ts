import { describe, it, expect, beforeEach, vi, type Mock } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useLabCatalogStore } from './labCatalog'
import api from '@/services/api'
import type { LabProvider, LabProviderTest, PanelResolution } from '@/types/lab'

vi.mock('@/services/api', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn()
  }
}))

const mockedApi = api as unknown as { get: Mock; post: Mock; put: Mock; delete: Mock }

function ok<T>(data: T) {
  return { data: { success: true, data } }
}

describe('useLabCatalogStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('fetchProviders unwraps ApiResponse and stores the list', async () => {
    const providers: LabProvider[] = [
      { id: 1, name: 'CLONY', code: 'CLONY', active: true },
      { id: 2, name: 'HERRERA', code: null, active: true }
    ]
    mockedApi.get.mockResolvedValueOnce(ok(providers))

    const store = useLabCatalogStore()
    await store.fetchProviders(true)

    expect(mockedApi.get).toHaveBeenCalledWith('/v1/lab/providers', {
      params: { activeOnly: true }
    })
    expect(store.providers).toEqual(providers)
  })

  it('createProvider POSTs and returns the created provider', async () => {
    const created: LabProvider = { id: 9, name: 'Bio', code: null, active: true }
    mockedApi.post.mockResolvedValueOnce(ok(created))

    const store = useLabCatalogStore()
    const result = await store.createProvider({ name: 'Bio', code: null, active: true })

    expect(mockedApi.post).toHaveBeenCalledWith('/v1/lab/providers', {
      name: 'Bio',
      code: null,
      active: true
    })
    expect(result).toEqual(created)
  })

  it('deleteProvider DELETEs the provider endpoint', async () => {
    mockedApi.delete.mockResolvedValueOnce({ data: { success: true } })
    const store = useLabCatalogStore()
    await store.deleteProvider(3)
    expect(mockedApi.delete).toHaveBeenCalledWith('/v1/lab/providers/3')
  })

  it('fetchProviderTests caches rows per provider and getProviderTests reads the cache', async () => {
    const rows: LabProviderTest[] = [
      {
        id: 11,
        providerId: 1,
        labTestId: 3,
        labTestName: 'Hematología',
        displayName: 'Hemograma',
        cost: 40,
        salesPrice: 75,
        active: true
      }
    ]
    mockedApi.get.mockResolvedValueOnce(ok(rows))

    const store = useLabCatalogStore()
    const returned = await store.fetchProviderTests(1, true)

    expect(mockedApi.get).toHaveBeenCalledWith('/v1/lab/providers/1/tests', {
      params: { activeOnly: true }
    })
    expect(returned).toEqual(rows)
    expect(store.getProviderTests(1)).toEqual(rows)
    // Unknown provider id returns an empty array.
    expect(store.getProviderTests(999)).toEqual([])
    expect(store.getProviderTests(null)).toEqual([])
  })

  it('resolvePanel calls the resolve endpoint with providerId and returns matched + unmatched', async () => {
    const resolution: PanelResolution = {
      panelId: 5,
      providerId: 2,
      matched: [
        { labProviderTestId: 21, labTestId: 3, displayName: 'Hematología', salesPrice: 85 }
      ],
      unmatchedTests: [{ labTestId: 15, name: 'Panel de drogas en sangre' }]
    }
    mockedApi.get.mockResolvedValueOnce(ok(resolution))

    const store = useLabCatalogStore()
    const result = await store.resolvePanel(5, 2)

    expect(mockedApi.get).toHaveBeenCalledWith('/v1/lab/panels/5/resolve', {
      params: { providerId: 2 }
    })
    expect(result.matched).toHaveLength(1)
    expect(result.unmatchedTests[0]?.name).toContain('drogas en sangre')
  })

  it('throws when the API responds with success=false', async () => {
    mockedApi.get.mockResolvedValueOnce({ data: { success: false, message: 'boom' } })
    const store = useLabCatalogStore()
    await expect(store.fetchProviders()).rejects.toThrow('boom')
  })
})
