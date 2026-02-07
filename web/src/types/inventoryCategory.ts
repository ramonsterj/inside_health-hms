export interface InventoryCategory {
  id: number
  name: string
  description: string | null
  displayOrder: number
  active: boolean
  createdAt: string | null
  updatedAt: string | null
}

export interface InventoryCategorySummary {
  id: number
  name: string
}

export interface CreateInventoryCategoryRequest {
  name: string
  description?: string | null
  displayOrder?: number
  active?: boolean
}

export interface UpdateInventoryCategoryRequest {
  name: string
  description?: string | null
  displayOrder?: number
  active?: boolean
}
