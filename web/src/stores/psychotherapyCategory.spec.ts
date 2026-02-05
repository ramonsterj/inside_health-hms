import { describe, it, expect, beforeEach, vi, afterEach, type Mock } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { usePsychotherapyCategoryStore } from './psychotherapyCategory'
import api from '@/services/api'
import type { PsychotherapyCategory } from '@/types/psychotherapy'

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

const mockCategory: PsychotherapyCategory = {
  id: 1,
  name: 'Taller',
  description: 'Workshop activities',
  displayOrder: 1,
  active: true,
  createdAt: '2026-02-05T10:00:00Z',
  updatedAt: '2026-02-05T10:00:00Z'
}

const mockCategories: PsychotherapyCategory[] = [
  {
    id: 1,
    name: 'Taller',
    description: 'Workshop activities',
    displayOrder: 1,
    active: true,
    createdAt: null,
    updatedAt: null
  },
  {
    id: 2,
    name: 'Sesión individual',
    description: 'Private one-on-one sessions',
    displayOrder: 2,
    active: true,
    createdAt: null,
    updatedAt: null
  },
  {
    id: 3,
    name: 'Terapia grupal',
    description: 'Group therapy sessions',
    displayOrder: 3,
    active: false,
    createdAt: null,
    updatedAt: null
  }
]

describe('usePsychotherapyCategoryStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  afterEach(() => {
    vi.resetAllMocks()
  })

  describe('initial state', () => {
    it('should start with empty state', () => {
      const store = usePsychotherapyCategoryStore()

      expect(store.categories).toEqual([])
      expect(store.activeCategories).toEqual([])
      expect(store.currentCategory).toBeNull()
      expect(store.loading).toBe(false)
    })
  })

  describe('fetchCategories', () => {
    it('should fetch all categories and update state', async () => {
      mockedApi.get.mockResolvedValueOnce({
        data: {
          success: true,
          data: mockCategories
        }
      })

      const store = usePsychotherapyCategoryStore()
      await store.fetchCategories()

      expect(mockedApi.get).toHaveBeenCalledWith('/v1/admin/psychotherapy-categories')
      expect(store.categories).toHaveLength(3)
      expect(store.categories[0]).toEqual(mockCategories[0])
    })

    it('should handle loading state during fetch', async () => {
      let resolvePromise: (value: unknown) => void = () => {}
      const promise = new Promise(resolve => {
        resolvePromise = resolve
      })

      mockedApi.get.mockReturnValueOnce(promise)

      const store = usePsychotherapyCategoryStore()
      const fetchPromise = store.fetchCategories()

      expect(store.loading).toBe(true)

      resolvePromise({
        data: { success: true, data: mockCategories }
      })

      await fetchPromise
      expect(store.loading).toBe(false)
    })
  })

  describe('fetchActiveCategories', () => {
    it('should fetch only active categories', async () => {
      const activeCategories = mockCategories.filter(c => c.active)
      mockedApi.get.mockResolvedValueOnce({
        data: {
          success: true,
          data: activeCategories
        }
      })

      const store = usePsychotherapyCategoryStore()
      await store.fetchActiveCategories()

      expect(mockedApi.get).toHaveBeenCalledWith('/v1/psychotherapy-categories')
      expect(store.activeCategories).toHaveLength(2)
      expect(store.activeCategories.every(c => c.active)).toBe(true)
    })
  })

  describe('fetchCategory', () => {
    it('should fetch a single category by id', async () => {
      mockedApi.get.mockResolvedValueOnce({
        data: {
          success: true,
          data: mockCategory
        }
      })

      const store = usePsychotherapyCategoryStore()
      const result = await store.fetchCategory(1)

      expect(mockedApi.get).toHaveBeenCalledWith('/v1/admin/psychotherapy-categories/1')
      expect(store.currentCategory).toEqual(mockCategory)
      expect(result).toEqual(mockCategory)
    })

    it('should throw error when category not found', async () => {
      mockedApi.get.mockResolvedValueOnce({
        data: {
          success: false,
          message: 'Category not found'
        }
      })

      const store = usePsychotherapyCategoryStore()

      await expect(store.fetchCategory(999)).rejects.toThrow('Category not found')
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

      const store = usePsychotherapyCategoryStore()
      const result = await store.createCategory(newCategory)

      expect(mockedApi.post).toHaveBeenCalledWith('/v1/admin/psychotherapy-categories', newCategory)
      expect(result.name).toBe('New Category')
    })

    it('should throw error on creation failure', async () => {
      mockedApi.post.mockResolvedValueOnce({
        data: {
          success: false,
          message: 'Category with this name already exists'
        }
      })

      const store = usePsychotherapyCategoryStore()

      await expect(
        store.createCategory({
          name: 'Taller',
          description: 'Duplicate',
          displayOrder: 1,
          active: true
        })
      ).rejects.toThrow('Category with this name already exists')
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

      const store = usePsychotherapyCategoryStore()
      const result = await store.updateCategory(1, updateData)

      expect(mockedApi.put).toHaveBeenCalledWith('/v1/admin/psychotherapy-categories/1', updateData)
      expect(result.name).toBe('Updated Name')
      expect(store.currentCategory?.name).toBe('Updated Name')
    })
  })

  describe('deleteCategory', () => {
    it('should delete a category', async () => {
      mockedApi.delete.mockResolvedValueOnce({
        data: { success: true }
      })

      const store = usePsychotherapyCategoryStore()
      await store.deleteCategory(1)

      expect(mockedApi.delete).toHaveBeenCalledWith('/v1/admin/psychotherapy-categories/1')
    })

    it('should throw error when category is in use', async () => {
      mockedApi.delete.mockResolvedValueOnce({
        data: {
          success: false,
          message: 'Cannot delete category that is in use by existing activities'
        }
      })

      const store = usePsychotherapyCategoryStore()

      await expect(store.deleteCategory(1)).rejects.toThrow(
        'Cannot delete category that is in use by existing activities'
      )
    })
  })

  describe('clearCurrentCategory', () => {
    it('should clear the current category', async () => {
      mockedApi.get.mockResolvedValueOnce({
        data: { success: true, data: mockCategory }
      })

      const store = usePsychotherapyCategoryStore()
      await store.fetchCategory(1)
      expect(store.currentCategory).not.toBeNull()

      store.clearCurrentCategory()
      expect(store.currentCategory).toBeNull()
    })
  })
})
