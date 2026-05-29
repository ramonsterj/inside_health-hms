import { describe, it, expect, beforeEach, vi, afterEach, type Mock } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useWarehouseStore } from './warehouse'
import api from '@/services/api'
import type { Warehouse, WarehouseStock } from '@/types/warehouse'
import { InventoryKind } from '@/types/inventoryItem'

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
  put: Mock
  delete: Mock
}

const mockWarehouse: Warehouse = {
  id: 1,
  code: 'MAIN',
  name: 'Main Warehouse',
  description: 'Central stock',
  active: true,
  createdAt: '2026-05-01T10:00:00Z',
  updatedAt: '2026-05-01T10:00:00Z'
}

const mockStock: WarehouseStock = {
  itemId: 10,
  name: 'Gauze',
  sku: 'SKU-10',
  kind: InventoryKind.SUPPLY,
  price: 2.5,
  restockLevel: 5,
  quantity: 3,
  lowStock: true
}

describe('useWarehouseStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  afterEach(() => {
    vi.resetAllMocks()
  })

  it('should start with empty state', () => {
    const store = useWarehouseStore()
    expect(store.warehouses).toEqual([])
    expect(store.stock).toEqual([])
    expect(store.totalStock).toBe(0)
    expect(store.loading).toBe(false)
  })

  it('should fetch warehouses', async () => {
    mockedApi.get.mockResolvedValueOnce({ data: { success: true, data: [mockWarehouse] } })

    const store = useWarehouseStore()
    await store.fetchWarehouses()

    expect(mockedApi.get).toHaveBeenCalledWith('/v1/warehouses')
    expect(store.warehouses).toHaveLength(1)
    expect(store.warehouses[0]!.code).toBe('MAIN')
  })

  it('should create a warehouse', async () => {
    const payload = { code: 'PHARM', name: 'Pharmacy', description: null, active: true }
    mockedApi.post.mockResolvedValueOnce({
      data: { success: true, data: { ...mockWarehouse, id: 2, code: 'PHARM' } }
    })

    const store = useWarehouseStore()
    const result = await store.createWarehouse(payload)

    expect(mockedApi.post).toHaveBeenCalledWith('/v1/warehouses', payload)
    expect(result.code).toBe('PHARM')
  })

  it('should update a warehouse', async () => {
    const payload = { name: 'Renamed', description: null, active: false }
    mockedApi.put.mockResolvedValueOnce({
      data: { success: true, data: { ...mockWarehouse, name: 'Renamed', active: false } }
    })

    const store = useWarehouseStore()
    const result = await store.updateWarehouse(1, payload)

    expect(mockedApi.put).toHaveBeenCalledWith('/v1/warehouses/1', payload)
    expect(result.name).toBe('Renamed')
  })

  it('should delete a warehouse', async () => {
    mockedApi.delete.mockResolvedValueOnce({ data: { success: true } })

    const store = useWarehouseStore()
    await store.deleteWarehouse(1)

    expect(mockedApi.delete).toHaveBeenCalledWith('/v1/warehouses/1')
  })

  it('should throw when delete fails', async () => {
    mockedApi.delete.mockResolvedValueOnce({ data: { success: false, message: 'not empty' } })

    const store = useWarehouseStore()
    await expect(store.deleteWarehouse(1)).rejects.toThrow('not empty')
  })

  it('should fetch stock with default pagination', async () => {
    mockedApi.get.mockResolvedValueOnce({
      data: {
        success: true,
        data: { content: [mockStock], page: { totalElements: 1, totalPages: 1, size: 20, number: 0 } }
      }
    })

    const store = useWarehouseStore()
    await store.fetchStock(1)

    expect(mockedApi.get).toHaveBeenCalledWith('/v1/warehouses/1/stock', {
      params: { page: 0, size: 20 }
    })
    expect(store.stock).toHaveLength(1)
    expect(store.totalStock).toBe(1)
  })

  it('should fetch stock with search and lowStockOnly filters', async () => {
    mockedApi.get.mockResolvedValueOnce({
      data: {
        success: true,
        data: { content: [], page: { totalElements: 0, totalPages: 0, size: 10, number: 1 } }
      }
    })

    const store = useWarehouseStore()
    await store.fetchStock(2, { search: 'gauze', lowStockOnly: true, page: 1, size: 10 })

    expect(mockedApi.get).toHaveBeenCalledWith('/v1/warehouses/2/stock', {
      params: { page: 1, size: 10, search: 'gauze', lowStockOnly: true }
    })
  })
})
