import { describe, it, expect, beforeEach, vi, afterEach, type Mock } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useInventoryCategoryStore } from './inventoryCategory'
import api from '@/services/api'
import type { InventoryCategory } from '@/types/inventoryCategory'

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

const mockCategory: InventoryCategory = {
  id: 1,
  name: 'Medicamentos',
  description: 'Medication and pharmaceutical supplies',
  displayOrder: 1,
  active: true,
  createdAt: '2026-02-06T10:00:00Z',
  updatedAt: '2026-02-06T10:00:00Z'
}

const mockCategories: InventoryCategory[] = [
  mockCategory,
  {
    id: 2,
    name: 'Material y Equipo',
    description: 'Materials and equipment',
    displayOrder: 2,
    active: true,
    createdAt: null,
    updatedAt: null
  },
  {
    id: 3,
    name: 'Inactive Category',
    description: null,
    displayOrder: 3,
    active: false,
    createdAt: null,
    updatedAt: null
  }
]

describe('useInventoryCategoryStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  afterEach(() => {
    vi.resetAllMocks()
  })

  describe('initial state', () => {
    it('should start with empty state', () => {
      const store = useInventoryCategoryStore()

      expect(store.categories).toEqual([])
      expect(store.activeCategories).toEqual([])
      expect(store.currentCategory).toBeNull()
      expect(store.loading).toBe(false)
    })
  })

  describe('fetchCategories', () => {
    it('should fetch all categories and update state', async () => {
      mockedApi.get.mockResolvedValueOnce({
        data: { success: true, data: mockCategories }
      })

      const store = useInventoryCategoryStore()
      await store.fetchCategories()

      expect(mockedApi.get).toHaveBeenCalledWith('/v1/admin/inventory-categories')
      expect(store.categories).toHaveLength(3)
    })
  })

  describe('fetchActiveCategories', () => {
    it('should fetch only active categories', async () => {
      const activeCategories = mockCategories.filter((c) => c.active)
      mockedApi.get.mockResolvedValueOnce({
        data: { success: true, data: activeCategories }
      })

      const store = useInventoryCategoryStore()
      await store.fetchActiveCategories()

      expect(mockedApi.get).toHaveBeenCalledWith('/v1/inventory-categories')
      expect(store.activeCategories).toHaveLength(2)
    })
  })

  describe('fetchCategory', () => {
    it('should fetch a single category by id', async () => {
      mockedApi.get.mockResolvedValueOnce({
        data: { success: true, data: mockCategory }
      })

      const store = useInventoryCategoryStore()
      const result = await store.fetchCategory(1)

      expect(mockedApi.get).toHaveBeenCalledWith('/v1/admin/inventory-categories/1')
      expect(store.currentCategory).toEqual(mockCategory)
      expect(result).toEqual(mockCategory)
    })
  })

  describe('createCategory', () => {
    it('should create a new category', async () => {
      const newCategory = {
        name: 'New Category',
        description: 'Test description',
        displayOrder: 10,
        active: true
      }

      mockedApi.post.mockResolvedValueOnce({
        data: {
          success: true,
          data: { id: 4, ...newCategory, createdAt: null, updatedAt: null }
        }
      })

      const store = useInventoryCategoryStore()
      const result = await store.createCategory(newCategory)

      expect(mockedApi.post).toHaveBeenCalledWith('/v1/admin/inventory-categories', newCategory)
      expect(result.name).toBe('New Category')
    })
  })

  describe('updateCategory', () => {
    it('should update an existing category', async () => {
      const updateData = {
        name: 'Updated Name',
        description: 'Updated description',
        displayOrder: 5,
        active: false
      }

      mockedApi.put.mockResolvedValueOnce({
        data: {
          success: true,
          data: { id: 1, ...updateData, createdAt: null, updatedAt: null }
        }
      })

      const store = useInventoryCategoryStore()
      const result = await store.updateCategory(1, updateData)

      expect(mockedApi.put).toHaveBeenCalledWith('/v1/admin/inventory-categories/1', updateData)
      expect(result.name).toBe('Updated Name')
    })
  })

  describe('deleteCategory', () => {
    it('should delete a category', async () => {
      mockedApi.delete.mockResolvedValueOnce({
        data: { success: true }
      })

      const store = useInventoryCategoryStore()
      await store.deleteCategory(1)

      expect(mockedApi.delete).toHaveBeenCalledWith('/v1/admin/inventory-categories/1')
    })

    it('should throw error when category has items', async () => {
      mockedApi.delete.mockResolvedValueOnce({
        data: {
          success: false,
          message: 'Cannot delete category that has inventory items'
        }
      })

      const store = useInventoryCategoryStore()

      await expect(store.deleteCategory(1)).rejects.toThrow(
        'Cannot delete category that has inventory items'
      )
    })
  })

  describe('clearCurrentCategory', () => {
    it('should clear the current category', async () => {
      mockedApi.get.mockResolvedValueOnce({
        data: { success: true, data: mockCategory }
      })

      const store = useInventoryCategoryStore()
      await store.fetchCategory(1)
      expect(store.currentCategory).not.toBeNull()

      store.clearCurrentCategory()
      expect(store.currentCategory).toBeNull()
    })
  })
})
