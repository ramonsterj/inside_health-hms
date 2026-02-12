import type { UserSummary } from './patient'
import type { InventoryCategorySummary } from './inventoryCategory'

export enum PricingType {
  FLAT = 'FLAT',
  TIME_BASED = 'TIME_BASED'
}

export enum TimeUnit {
  MINUTES = 'MINUTES',
  HOURS = 'HOURS'
}

export enum MovementType {
  ENTRY = 'ENTRY',
  EXIT = 'EXIT'
}

export interface InventoryItem {
  id: number
  name: string
  description: string | null
  category: InventoryCategorySummary
  price: number
  cost: number
  quantity: number
  restockLevel: number
  pricingType: PricingType
  timeUnit: TimeUnit | null
  timeInterval: number | null
  active: boolean
  createdAt: string | null
  updatedAt: string | null
  createdBy: UserSummary | null
  updatedBy: UserSummary | null
}

export interface InventoryMovement {
  id: number
  itemId: number
  type: MovementType
  quantity: number
  previousQuantity: number
  newQuantity: number
  notes: string | null
  createdAt: string | null
  createdBy: UserSummary | null
}

export interface CreateInventoryItemRequest {
  name: string
  description?: string | null
  categoryId: number
  price: number
  cost: number
  restockLevel?: number
  pricingType?: PricingType
  timeUnit?: TimeUnit | null
  timeInterval?: number | null
  active?: boolean
}

export interface UpdateInventoryItemRequest {
  name: string
  description?: string | null
  categoryId: number
  price: number
  cost: number
  restockLevel?: number
  pricingType?: PricingType
  timeUnit?: TimeUnit | null
  timeInterval?: number | null
  active?: boolean
}

export interface CreateInventoryMovementRequest {
  type: MovementType
  quantity: number
  notes?: string | null
  admissionId?: number | null
}
