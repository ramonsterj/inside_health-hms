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

export enum InventoryKind {
  DRUG = 'DRUG',
  SUPPLY = 'SUPPLY',
  EQUIPMENT = 'EQUIPMENT',
  SERVICE = 'SERVICE',
  PERSONNEL = 'PERSONNEL',
  FOOD = 'FOOD'
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
  kind: InventoryKind
  sku: string | null
  lotTrackingEnabled: boolean
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
  lotId: number | null
  lotNumber: string | null
  lotExpirationDate: string | null
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
  kind?: InventoryKind
  sku?: string | null
  lotTrackingEnabled?: boolean
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
  // Omit kind / sku / lotTrackingEnabled to preserve the item's current values
  // (the backend treats null/absent as "no change" on update).
  kind?: InventoryKind | null
  sku?: string | null
  lotTrackingEnabled?: boolean | null
}

export interface CreateInventoryMovementRequest {
  type: MovementType
  quantity: number
  notes?: string | null
  admissionId?: number | null
  lotId?: number | null
  lotNumber?: string | null
  expirationDate?: string | null
  supplier?: string | null
}
