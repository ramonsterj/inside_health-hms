export enum ChargeType {
  MEDICATION = 'MEDICATION',
  ROOM = 'ROOM',
  PROCEDURE = 'PROCEDURE',
  LAB = 'LAB',
  SERVICE = 'SERVICE',
  ADJUSTMENT = 'ADJUSTMENT'
}

export interface PatientCharge {
  id: number
  admissionId: number
  chargeType: ChargeType
  description: string
  quantity: number
  unitPrice: number
  totalAmount: number
  inventoryItemName: string | null
  roomNumber: string | null
  invoiced: boolean
  reason: string | null
  chargeDate: string
  createdAt: string | null
  createdByName: string | null
}

export interface DailyChargeItem {
  id: number
  chargeType: ChargeType
  description: string
  quantity: number
  unitPrice: number
  totalAmount: number
}

export interface DailyChargeGroup {
  date: string
  charges: DailyChargeItem[]
  dailyTotal: number
  cumulativeTotal: number
}

export interface AdmissionBalance {
  admissionId: number
  patientName: string
  admissionDate: string
  totalBalance: number
  dailyBreakdown: DailyChargeGroup[]
}

export interface ChargeSummaryItem {
  chargeType: ChargeType
  count: number
  subtotal: number
}

export interface Invoice {
  id: number
  invoiceNumber: string
  admissionId: number
  patientName: string
  admissionDate: string
  dischargeDate: string | null
  totalAmount: number
  chargeCount: number
  chargeSummary: ChargeSummaryItem[]
  generatedAt: string | null
  generatedByName: string | null
}

export interface CreateChargeRequest {
  chargeType: ChargeType
  description: string
  quantity: number
  unitPrice: number
  inventoryItemId?: number | null
}

export interface CreateAdjustmentRequest {
  description: string
  amount: number
  reason: string
}

export type TagSeverity = 'success' | 'info' | 'warn' | 'danger' | 'secondary'

const chargeTypeSeverityMap: Record<ChargeType, TagSeverity> = {
  [ChargeType.ROOM]: 'info',
  [ChargeType.MEDICATION]: 'success',
  [ChargeType.PROCEDURE]: 'warn',
  [ChargeType.LAB]: 'secondary',
  [ChargeType.SERVICE]: 'info',
  [ChargeType.ADJUSTMENT]: 'danger'
}

export function getChargeTypeSeverity(type: ChargeType): TagSeverity {
  return chargeTypeSeverityMap[type] || 'secondary'
}
