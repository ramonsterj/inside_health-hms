export enum DosageForm {
  TABLET = 'TABLET',
  CAPSULE = 'CAPSULE',
  AMPOULE = 'AMPOULE',
  SYRUP = 'SYRUP',
  DROPS = 'DROPS',
  CREAM = 'CREAM',
  INJECTION = 'INJECTION',
  POWDER = 'POWDER',
  PATCH = 'PATCH',
  OTHER = 'OTHER'
}

export enum AdministrationRoute {
  ORAL = 'ORAL',
  IV = 'IV',
  IM = 'IM',
  SC = 'SC',
  TOPICAL = 'TOPICAL',
  INHALATION = 'INHALATION',
  RECTAL = 'RECTAL',
  SUBLINGUAL = 'SUBLINGUAL',
  OTHER = 'OTHER'
}

export enum MedicationSection {
  PSIQUIATRICO = 'PSIQUIATRICO',
  NO_PSIQUIATRICO = 'NO_PSIQUIATRICO',
  JARABE_GOTAS = 'JARABE_GOTAS',
  AMPOLLA = 'AMPOLLA'
}

export enum MedicationReviewStatus {
  CONFIRMED = 'CONFIRMED',
  NEEDS_REVIEW = 'NEEDS_REVIEW'
}

export enum LotExpiryStatus {
  EXPIRED = 'EXPIRED',
  RED = 'RED',
  YELLOW = 'YELLOW',
  GREEN = 'GREEN',
  NO_EXPIRY = 'NO_EXPIRY'
}

export interface Medication {
  id: number
  itemId: number
  name: string
  description: string | null
  sku: string | null
  price: number
  cost: number
  restockLevel: number
  quantity: number
  active: boolean
  genericName: string
  commercialName: string | null
  strength: string | null
  dosageForm: DosageForm
  route: AdministrationRoute | null
  controlled: boolean
  atcCode: string | null
  section: MedicationSection
  reviewStatus: MedicationReviewStatus
  reviewNotes: string | null
}

export interface CreateMedicationRequest {
  name: string
  description?: string | null
  price: number
  cost: number
  sku?: string | null
  restockLevel?: number
  genericName: string
  commercialName?: string | null
  strength?: string | null
  dosageForm: DosageForm
  route?: AdministrationRoute | null
  controlled?: boolean
  atcCode?: string | null
  section: MedicationSection
  active?: boolean
}

export type UpdateMedicationRequest = CreateMedicationRequest

export interface InventoryLot {
  id: number
  itemId: number
  itemName: string | null
  itemSku: string | null
  lotNumber: string | null
  expirationDate: string
  quantityOnHand: number
  receivedAt: string
  supplier: string | null
  notes: string | null
  recalled: boolean
  recalledReason: string | null
  syntheticLegacy: boolean
  createdAt: string | null
  updatedAt: string | null
}

export interface CreateInventoryLotRequest {
  lotNumber?: string | null
  expirationDate: string
  quantityOnHand: number
  receivedAt: string
  supplier?: string | null
  notes?: string | null
}

export interface UpdateInventoryLotRequest {
  lotNumber?: string | null
  expirationDate: string
  supplier?: string | null
  notes?: string | null
  recalled?: boolean
  recalledReason?: string | null
}

export interface ExpiryReportRow {
  lotId: number
  itemId: number
  sku: string | null
  genericName: string | null
  commercialName: string | null
  strength: string | null
  section: MedicationSection | null
  lotNumber: string | null
  expirationDate: string
  daysToExpiry: number | null
  status: LotExpiryStatus
  quantityOnHand: number
  recalled: boolean
}

export interface ExpiryReport {
  generatedAt: string
  totals: Record<LotExpiryStatus, number>
  items: ExpiryReportRow[]
}
