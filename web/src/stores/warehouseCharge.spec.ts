import { describe, it, expect, beforeEach, vi, afterEach, type Mock } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useWarehouseChargeStore } from './warehouseCharge'
import api from '@/services/api'
import type { WarehouseCharge, CreateWarehouseChargeRequest } from '@/types/warehouse'

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

const mockCharge: WarehouseCharge = {
  id: 1,
  warehouse: { id: 1, code: 'MAIN', name: 'Main' },
  item: { id: 10, name: 'Gauze', sku: 'SKU-10' },
  admission: { id: 99, patientName: 'Jane Roe', roomNumber: '101' },
  quantity: 2,
  amount: 5,
  reason: 'Wound care',
  notes: null,
  chargeId: 500,
  createdBy: { id: 7, username: 'jdoe', firstName: 'John', lastName: 'Doe' },
  createdAt: '2026-05-10T12:00:00Z'
}

describe('useWarehouseChargeStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  afterEach(() => {
    vi.resetAllMocks()
  })

  it('should start with empty state', () => {
    const store = useWarehouseChargeStore()
    expect(store.charges).toEqual([])
    expect(store.totalCharges).toBe(0)
    expect(store.loading).toBe(false)
  })

  it('should fetch charges with default pagination', async () => {
    mockedApi.get.mockResolvedValueOnce({
      data: {
        success: true,
        data: {
          content: [mockCharge],
          page: { totalElements: 1, totalPages: 1, size: 20, number: 0 }
        }
      }
    })

    const store = useWarehouseChargeStore()
    await store.fetchCharges()

    expect(mockedApi.get).toHaveBeenCalledWith('/v1/warehouse-charges', {
      params: { page: 0, size: 20 }
    })
    expect(store.charges).toHaveLength(1)
    expect(store.totalCharges).toBe(1)
  })

  it('should fetch charges with warehouse and admission filters', async () => {
    mockedApi.get.mockResolvedValueOnce({
      data: {
        success: true,
        data: { content: [], page: { totalElements: 0, totalPages: 0, size: 20, number: 0 } }
      }
    })

    const store = useWarehouseChargeStore()
    await store.fetchCharges(0, 20, 1, 99)

    expect(mockedApi.get).toHaveBeenCalledWith('/v1/warehouse-charges', {
      params: { page: 0, size: 20, warehouseId: 1, admissionId: 99 }
    })
  })

  it('should create a charge', async () => {
    const payload: CreateWarehouseChargeRequest = {
      warehouseId: 1,
      itemId: 10,
      lotId: null,
      admissionId: 99,
      quantity: 2,
      reason: 'Wound care',
      notes: null
    }
    mockedApi.post.mockResolvedValueOnce({ data: { success: true, data: mockCharge } })

    const store = useWarehouseChargeStore()
    const result = await store.createCharge(payload)

    expect(mockedApi.post).toHaveBeenCalledWith('/v1/warehouse-charges', payload)
    expect(result.id).toBe(1)
  })

  it('should throw when create fails', async () => {
    mockedApi.post.mockResolvedValueOnce({ data: { success: false, message: 'unassigned' } })

    const store = useWarehouseChargeStore()
    await expect(
      store.createCharge({
        warehouseId: 1,
        itemId: 10,
        admissionId: 99,
        quantity: 2,
        reason: 'x'
      })
    ).rejects.toThrow('unassigned')
  })
})
