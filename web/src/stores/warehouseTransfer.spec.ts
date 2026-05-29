import { describe, it, expect, beforeEach, vi, afterEach, type Mock } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useWarehouseTransferStore } from './warehouseTransfer'
import api from '@/services/api'
import type { Transfer, CreateTransferRequest } from '@/types/warehouse'

vi.mock('@/services/api', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn()
  }
}))

const mockedApi = api as unknown as {
  get: Mock
  post: Mock
}

const mockTransfer: Transfer = {
  id: 1,
  status: 'COMPLETED',
  sourceWarehouse: { id: 1, code: 'MAIN', name: 'Main' },
  destinationWarehouse: { id: 2, code: 'PHARM', name: 'Pharmacy' },
  item: { id: 10, name: 'Gauze', sku: 'SKU-10' },
  lot: null,
  quantity: 5,
  notes: null,
  issuedBy: { id: 7, username: 'jdoe', firstName: 'John', lastName: 'Doe' },
  issuedAt: '2026-05-10T12:00:00Z',
  completedAt: '2026-05-10T12:01:00Z'
}

describe('useWarehouseTransferStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  afterEach(() => {
    vi.resetAllMocks()
  })

  it('should start with empty state', () => {
    const store = useWarehouseTransferStore()
    expect(store.transfers).toEqual([])
    expect(store.totalTransfers).toBe(0)
    expect(store.loading).toBe(false)
  })

  it('should fetch transfers with default pagination', async () => {
    mockedApi.get.mockResolvedValueOnce({
      data: {
        success: true,
        data: {
          content: [mockTransfer],
          page: { totalElements: 1, totalPages: 1, size: 20, number: 0 }
        }
      }
    })

    const store = useWarehouseTransferStore()
    await store.fetchTransfers()

    expect(mockedApi.get).toHaveBeenCalledWith('/v1/warehouse-transfers', {
      params: { page: 0, size: 20 }
    })
    expect(store.transfers).toHaveLength(1)
    expect(store.totalTransfers).toBe(1)
  })

  it('should fetch transfers with warehouse and item filters', async () => {
    mockedApi.get.mockResolvedValueOnce({
      data: {
        success: true,
        data: { content: [], page: { totalElements: 0, totalPages: 0, size: 20, number: 0 } }
      }
    })

    const store = useWarehouseTransferStore()
    await store.fetchTransfers(0, 20, 1, 10)

    expect(mockedApi.get).toHaveBeenCalledWith('/v1/warehouse-transfers', {
      params: { page: 0, size: 20, warehouseId: 1, itemId: 10 }
    })
  })

  it('should create a transfer', async () => {
    const payload: CreateTransferRequest = {
      sourceWarehouseId: 1,
      destinationWarehouseId: 2,
      itemId: 10,
      lotId: null,
      quantity: 5,
      notes: null
    }
    mockedApi.post.mockResolvedValueOnce({ data: { success: true, data: mockTransfer } })

    const store = useWarehouseTransferStore()
    const result = await store.createTransfer(payload)

    expect(mockedApi.post).toHaveBeenCalledWith('/v1/warehouse-transfers', payload)
    expect(result.id).toBe(1)
  })

  it('should throw when create fails', async () => {
    mockedApi.post.mockResolvedValueOnce({ data: { success: false, message: 'out of stock' } })

    const store = useWarehouseTransferStore()
    await expect(
      store.createTransfer({
        sourceWarehouseId: 1,
        destinationWarehouseId: 2,
        itemId: 10,
        quantity: 5
      })
    ).rejects.toThrow('out of stock')
  })
})
