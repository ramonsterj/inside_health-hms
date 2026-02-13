export enum AdministrationStatus {
  GIVEN = 'GIVEN',
  MISSED = 'MISSED',
  REFUSED = 'REFUSED',
  HELD = 'HELD'
}

export interface MedicationAdministrationResponse {
  id: number
  medicalOrderId: number
  admissionId: number
  medication: string | null
  dosage: string | null
  route: string | null
  status: AdministrationStatus
  notes: string | null
  administeredAt: string
  administeredByName: string | null
  inventoryItemName: string | null
  billable: boolean
}

export interface CreateMedicationAdministrationRequest {
  status: AdministrationStatus
  notes?: string
}
