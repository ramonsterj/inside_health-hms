/**
 * Medical Record types for Clinical Histories, Progress Notes, and Medical Orders.
 * Matches backend DTOs from the medical record feature.
 */

// Enums matching backend
export enum MedicalOrderCategory {
  ORDENES_MEDICAS = 'ORDENES_MEDICAS',
  MEDICAMENTOS = 'MEDICAMENTOS',
  LABORATORIOS = 'LABORATORIOS',
  REFERENCIAS_MEDICAS = 'REFERENCIAS_MEDICAS',
  PRUEBAS_PSICOMETRICAS = 'PRUEBAS_PSICOMETRICAS',
  ACTIVIDAD_FISICA = 'ACTIVIDAD_FISICA',
  CUIDADOS_ESPECIALES = 'CUIDADOS_ESPECIALES',
  DIETA = 'DIETA',
  RESTRICCIONES_MOVILIDAD = 'RESTRICCIONES_MOVILIDAD',
  PERMISOS_VISITA = 'PERMISOS_VISITA',
  OTRAS = 'OTRAS'
}

export enum MedicalOrderStatus {
  ACTIVE = 'ACTIVE',
  DISCONTINUED = 'DISCONTINUED'
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

// Medical staff response (for audit info)
export interface MedicalStaffResponse {
  id: number
  salutation: string | null
  firstName: string | null
  lastName: string | null
  roles: string[]
}

// Clinical History
export interface ClinicalHistoryResponse {
  id: number
  admissionId: number
  reasonForAdmission: string | null
  historyOfPresentIllness: string | null
  psychiatricHistory: string | null
  medicalHistory: string | null
  familyHistory: string | null
  personalHistory: string | null
  substanceUseHistory: string | null
  legalHistory: string | null
  socialHistory: string | null
  developmentalHistory: string | null
  educationalOccupationalHistory: string | null
  sexualHistory: string | null
  religiousSpiritualHistory: string | null
  mentalStatusExam: string | null
  physicalExam: string | null
  diagnosticImpression: string | null
  treatmentPlan: string | null
  riskAssessment: string | null
  prognosis: string | null
  informedConsentNotes: string | null
  additionalNotes: string | null
  createdAt: string | null
  updatedAt: string | null
  createdBy: MedicalStaffResponse | null
  updatedBy: MedicalStaffResponse | null
}

export interface CreateClinicalHistoryRequest {
  reasonForAdmission?: string | null
  historyOfPresentIllness?: string | null
  psychiatricHistory?: string | null
  medicalHistory?: string | null
  familyHistory?: string | null
  personalHistory?: string | null
  substanceUseHistory?: string | null
  legalHistory?: string | null
  socialHistory?: string | null
  developmentalHistory?: string | null
  educationalOccupationalHistory?: string | null
  sexualHistory?: string | null
  religiousSpiritualHistory?: string | null
  mentalStatusExam?: string | null
  physicalExam?: string | null
  diagnosticImpression?: string | null
  treatmentPlan?: string | null
  riskAssessment?: string | null
  prognosis?: string | null
  informedConsentNotes?: string | null
  additionalNotes?: string | null
}

export interface UpdateClinicalHistoryRequest {
  reasonForAdmission?: string | null
  historyOfPresentIllness?: string | null
  psychiatricHistory?: string | null
  medicalHistory?: string | null
  familyHistory?: string | null
  personalHistory?: string | null
  substanceUseHistory?: string | null
  legalHistory?: string | null
  socialHistory?: string | null
  developmentalHistory?: string | null
  educationalOccupationalHistory?: string | null
  sexualHistory?: string | null
  religiousSpiritualHistory?: string | null
  mentalStatusExam?: string | null
  physicalExam?: string | null
  diagnosticImpression?: string | null
  treatmentPlan?: string | null
  riskAssessment?: string | null
  prognosis?: string | null
  informedConsentNotes?: string | null
  additionalNotes?: string | null
}

// Progress Notes
export interface ProgressNoteResponse {
  id: number
  admissionId: number
  subjectiveData: string | null
  objectiveData: string | null
  analysis: string | null
  actionPlans: string | null
  createdAt: string | null
  updatedAt: string | null
  createdBy: MedicalStaffResponse | null
  updatedBy: MedicalStaffResponse | null
}

export interface CreateProgressNoteRequest {
  subjectiveData?: string | null
  objectiveData?: string | null
  analysis?: string | null
  actionPlans?: string | null
}

export interface UpdateProgressNoteRequest {
  subjectiveData?: string | null
  objectiveData?: string | null
  analysis?: string | null
  actionPlans?: string | null
}

// Medical Orders
export interface MedicalOrderResponse {
  id: number
  admissionId: number
  category: MedicalOrderCategory
  startDate: string
  endDate: string | null
  medication: string | null
  dosage: string | null
  route: AdministrationRoute | null
  frequency: string | null
  schedule: string | null
  observations: string | null
  status: MedicalOrderStatus
  discontinuedAt: string | null
  discontinuedBy: MedicalStaffResponse | null
  createdAt: string | null
  updatedAt: string | null
  createdBy: MedicalStaffResponse | null
  updatedBy: MedicalStaffResponse | null
}

export interface CreateMedicalOrderRequest {
  category: MedicalOrderCategory
  startDate: string
  endDate?: string | null
  medication?: string | null
  dosage?: string | null
  route?: AdministrationRoute | null
  frequency?: string | null
  schedule?: string | null
  observations?: string | null
}

export interface UpdateMedicalOrderRequest {
  category?: MedicalOrderCategory
  startDate?: string
  endDate?: string | null
  medication?: string | null
  dosage?: string | null
  route?: AdministrationRoute | null
  frequency?: string | null
  schedule?: string | null
  observations?: string | null
}

// Grouped medical orders by category
export interface GroupedMedicalOrdersResponse {
  orders: Record<MedicalOrderCategory, MedicalOrderResponse[]>
}

// Clinical history field names for form iteration
export const CLINICAL_HISTORY_FIELDS = [
  'reasonForAdmission',
  'historyOfPresentIllness',
  'psychiatricHistory',
  'medicalHistory',
  'familyHistory',
  'personalHistory',
  'substanceUseHistory',
  'legalHistory',
  'socialHistory',
  'developmentalHistory',
  'educationalOccupationalHistory',
  'sexualHistory',
  'religiousSpiritualHistory',
  'mentalStatusExam',
  'physicalExam',
  'diagnosticImpression',
  'treatmentPlan',
  'riskAssessment',
  'prognosis',
  'informedConsentNotes',
  'additionalNotes'
] as const

export type ClinicalHistoryFieldName = (typeof CLINICAL_HISTORY_FIELDS)[number]
