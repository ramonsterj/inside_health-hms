import { describe, it, expect, beforeEach, vi, afterEach, type Mock } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useInventoryItemStore } from './inventoryItem'
import api from '@/services/api'
import { PricingType, MovementType } from '@/types/inventoryItem'
import type { InventoryItem, InventoryMovement } from '@/types/inventoryItem'

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

const mockItem: InventoryItem = {
  id: 1,
  name: 'Aspirina 500mg',
  description: 'Aspirin tablets 500mg',
  category: { id: 1, name: 'Medicamentos' },
  price: 5.0,
  cost: 2.5,
  quantity: 100,
  restockLevel: 50,
  pricingType: PricingType.FLAT,
  timeUnit: null,
  timeInterval: null,
  active: true,
  createdAt: '2026-02-06T10:00:00Z',
  updatedAt: '2026-02-06T10:00:00Z',
  createdBy: null,
  updatedBy: null
}

const mockMovement: InventoryMovement = {
  id: 1,
  itemId: 1,
  type: MovementType.ENTRY,
  quantity: 50,
  previousQuantity: 0,
  newQuantity: 50,
  notes: 'Initial stock',
  createdAt: '2026-02-06T10:00:00Z',
  createdBy: null
}

describe('useInventoryItemStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  afterEach(() => {
    vi.resetAllMocks()
  })

  describe('initial state', () => {
    it('should start with empty state', () => {
      const store = useInventoryItemStore()

      expect(store.items).toEqual([])
      expect(store.totalItems).toBe(0)
      expect(store.currentItem).toBeNull()
      expect(store.movements).toEqual([])
      expect(store.lowStockItems).toEqual([])
      expect(store.loading).toBe(false)
    })
  })

  describe('fetchItems', () => {
    const mockPageResponse = {
      content: [mockItem],
      page: { totalElements: 1, totalPages: 1, size: 20, number: 0 }
    }

    it('should fetch items with default pagination', async () => {
      mockedApi.get.mockResolvedValueOnce({
        data: { success: true, data: mockPageResponse }
      })

      const store = useInventoryItemStore()
      await store.fetchItems()

      expect(mockedApi.get).toHaveBeenCalledWith('/v1/admin/inventory-items', {
        params: { page: 0, size: 20 }
      })
      expect(store.items).toHaveLength(1)
      expect(store.totalItems).toBe(1)
    })

    it('should fetch items with category filter', async () => {
      mockedApi.get.mockResolvedValueOnce({
        data: { success: true, data: mockPageResponse }
      })

      const store = useInventoryItemStore()
      await store.fetchItems(0, 20, 1)

      expect(mockedApi.get).toHaveBeenCalledWith('/v1/admin/inventory-items', {
        params: { page: 0, size: 20, categoryId: 1 }
      })
    })

    it('should fetch items with search filter', async () => {
      mockedApi.get.mockResolvedValueOnce({
        data: { success: true, data: mockPageResponse }
      })

      const store = useInventoryItemStore()
      await store.fetchItems(0, 20, undefined, 'aspir')

      expect(mockedApi.get).toHaveBeenCalledWith('/v1/admin/inventory-items', {
        params: { page: 0, size: 20, search: 'aspir' }
      })
    })

    it('should fetch items with custom page and size', async () => {
      mockedApi.get.mockResolvedValueOnce({
        data: { success: true, data: mockPageResponse }
      })

      const store = useInventoryItemStore()
      await store.fetchItems(2, 10)

      expect(mockedApi.get).toHaveBeenCalledWith('/v1/admin/inventory-items', {
        params: { page: 2, size: 10 }
      })
    })
  })

  describe('fetchItem', () => {
    it('should fetch a single item by id', async () => {
      mockedApi.get.mockResolvedValueOnce({
        data: { success: true, data: mockItem }
      })

      const store = useInventoryItemStore()
      const result = await store.fetchItem(1)

      expect(mockedApi.get).toHaveBeenCalledWith('/v1/admin/inventory-items/1')
      expect(store.currentItem).toEqual(mockItem)
      expect(result).toEqual(mockItem)
    })
  })

  describe('createItem', () => {
    it('should create a new item', async () => {
      const newItem = {
        name: 'New Item',
        categoryId: 1,
        price: 10.0,
        cost: 5.0,
        restockLevel: 20,
        pricingType: PricingType.FLAT
      }

      mockedApi.post.mockResolvedValueOnce({
        data: { success: true, data: { ...mockItem, id: 2, name: 'New Item' } }
      })

      const store = useInventoryItemStore()
      const result = await store.createItem(newItem)

      expect(mockedApi.post).toHaveBeenCalledWith('/v1/admin/inventory-items', newItem)
      expect(result.name).toBe('New Item')
    })
  })

  describe('deleteItem', () => {
    it('should delete an item', async () => {
      mockedApi.delete.mockResolvedValueOnce({
        data: { success: true }
      })

      const store = useInventoryItemStore()
      await store.deleteItem(1)

      expect(mockedApi.delete).toHaveBeenCalledWith('/v1/admin/inventory-items/1')
    })
  })

  describe('fetchLowStock', () => {
    it('should fetch low stock items', async () => {
      mockedApi.get.mockResolvedValueOnce({
        data: { success: true, data: [mockItem] }
      })

      const store = useInventoryItemStore()
      await store.fetchLowStock()

      expect(mockedApi.get).toHaveBeenCalledWith('/v1/admin/inventory-items/low-stock')
      expect(store.lowStockItems).toHaveLength(1)
    })

    it('should fetch low stock with category filter', async () => {
      mockedApi.get.mockResolvedValueOnce({
        data: { success: true, data: [] }
      })

      const store = useInventoryItemStore()
      await store.fetchLowStock(1)

      expect(mockedApi.get).toHaveBeenCalledWith('/v1/admin/inventory-items/low-stock?categoryId=1')
    })
  })

  describe('movements', () => {
    it('should fetch movements for an item', async () => {
      mockedApi.get.mockResolvedValueOnce({
        data: { success: true, data: [mockMovement] }
      })

      const store = useInventoryItemStore()
      await store.fetchMovements(1)

      expect(mockedApi.get).toHaveBeenCalledWith('/v1/admin/inventory-items/1/movements')
      expect(store.movements).toHaveLength(1)
    })

    it('should create a movement', async () => {
      const movementData = {
        type: MovementType.ENTRY,
        quantity: 50,
        notes: 'Restock'
      }

      mockedApi.post.mockResolvedValueOnce({
        data: { success: true, data: mockMovement }
      })

      const store = useInventoryItemStore()
      const result = await store.createMovement(1, movementData)

      expect(mockedApi.post).toHaveBeenCalledWith(
        '/v1/admin/inventory-items/1/movements',
        movementData
      )
      expect(result.type).toBe('ENTRY')
      expect(result.quantity).toBe(50)
    })
  })

  describe('clearCurrentItem', () => {
    it('should clear the current item and movements', async () => {
      mockedApi.get.mockResolvedValueOnce({
        data: { success: true, data: mockItem }
      })

      const store = useInventoryItemStore()
      await store.fetchItem(1)
      expect(store.currentItem).not.toBeNull()

      store.clearCurrentItem()
      expect(store.currentItem).toBeNull()
      expect(store.movements).toEqual([])
    })
  })
})
