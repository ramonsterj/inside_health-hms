import type { InventoryKind } from './inventoryItem'

export interface WarehouseSummary {
  id: number
  code: string
  name: string
}

export interface Warehouse {
  id: number
  code: string
  name: string
  description: string | null
  active: boolean
  createdAt: string | null
  updatedAt: string | null
}

export interface CreateWarehouseRequest {
  code: string
  name: string
  description?: string | null
  active: boolean
}

export interface UpdateWarehouseRequest {
  name: string
  description?: string | null
  active: boolean
}

export interface WarehouseStock {
  itemId: number
  name: string
  sku: string | null
  kind: InventoryKind
  price: number
  restockLevel: number
  quantity: number
  lowStock: boolean
}

export interface WarehouseStockQuery {
  search?: string | null
  lowStockOnly?: boolean | null
  page?: number
  size?: number
}

export enum TransferStatus {
  PENDING = 'PENDING',
  COMPLETED = 'COMPLETED',
  CANCELLED = 'CANCELLED'
}

export interface TransferUserSummary {
  id: number
  username: string
  firstName: string | null
  lastName: string | null
}

export interface TransferItemSummary {
  id: number
  name: string
  sku: string | null
}

export interface TransferLotSummary {
  id: number
  lotNumber: string | null
  expirationDate: string | null
}

export interface Transfer {
  id: number
  status: string
  sourceWarehouse: WarehouseSummary
  destinationWarehouse: WarehouseSummary
  item: TransferItemSummary
  lot: TransferLotSummary | null
  quantity: number
  notes: string | null
  issuedBy: TransferUserSummary | null
  issuedAt: string | null
  completedAt: string | null
}

export interface CreateTransferRequest {
  sourceWarehouseId: number
  destinationWarehouseId: number
  itemId: number
  lotId?: number | null
  quantity: number
  notes?: string | null
}

export interface WarehouseChargeAdmissionSummary {
  id: number
  patientName: string
  roomNumber: string | null
}

export interface WarehouseCharge {
  id: number
  warehouse: WarehouseSummary
  item: TransferItemSummary
  admission: WarehouseChargeAdmissionSummary
  quantity: number
  amount: number
  reason: string
  notes: string | null
  chargeId: number | null
  createdBy: TransferUserSummary | null
  createdAt: string | null
}

export interface CreateWarehouseChargeRequest {
  warehouseId: number
  itemId: number
  lotId?: number | null
  admissionId: number
  quantity: number
  reason: string
  notes?: string | null
}
