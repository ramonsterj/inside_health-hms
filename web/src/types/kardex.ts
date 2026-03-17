import type { AdmissionType } from './admission'

export interface KardexAdmissionSummary {
  admissionId: number
  patientId: number
  patientName: string
  roomNumber: string | null
  triageCode: string | null
  triageColorCode: string | null
  admissionType: AdmissionType
  admissionDate: string
  daysAdmitted: number
  treatingPhysicianName: string

  activeMedicationCount: number
  medications: KardexMedicationSummary[]

  activeCareInstructionCount: number
  careInstructions: KardexCareInstruction[]

  latestVitalSigns: KardexVitalSignSummary | null
  hoursSinceLastVitals: number | null

  lastNursingNotePreview: string | null
  lastNursingNoteAt: string | null
}

export interface KardexMedicationSummary {
  orderId: number
  medication: string | null
  dosage: string | null
  route: string | null
  frequency: string | null
  schedule: string | null
  inventoryItemId: number | null
  inventoryItemName: string | null
  observations: string | null
  lastAdministration: KardexLastAdministration | null
}

export interface KardexLastAdministration {
  administeredAt: string
  status: string
  administeredByName: string | null
}

export interface KardexCareInstruction {
  orderId: number
  category: string
  startDate: string
  observations: string | null
}

export interface KardexVitalSignSummary {
  recordedAt: string
  systolicBp: number
  diastolicBp: number
  heartRate: number
  respiratoryRate: number
  temperature: number
  oxygenSaturation: number
  recordedByName: string | null
}
