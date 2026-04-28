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
  ACTIVA = 'ACTIVA',
  SOLICITADO = 'SOLICITADO',
  NO_AUTORIZADO = 'NO_AUTORIZADO',
  AUTORIZADO = 'AUTORIZADO',
  EN_PROCESO = 'EN_PROCESO',
  RESULTADOS_RECIBIDOS = 'RESULTADOS_RECIBIDOS',
  DESCONTINUADO = 'DESCONTINUADO'
}

export enum EmergencyAuthorizationReason {
  PATIENT_IN_CRISIS = 'PATIENT_IN_CRISIS',
  AFTER_HOURS_NO_ADMIN = 'AFTER_HOURS_NO_ADMIN',
  FAMILY_UNREACHABLE = 'FAMILY_UNREACHABLE',
  OTHER = 'OTHER'
}

// Categories that have an external execution + results phase. After authorize, these go
// AUTORIZADO -> EN_PROCESO (sample taken / patient referred / test administered) -> RESULTADOS_RECIBIDOS.
export const RESULTS_BEARING_CATEGORIES: readonly MedicalOrderCategory[] = [
  MedicalOrderCategory.LABORATORIOS,
  MedicalOrderCategory.REFERENCIAS_MEDICAS,
  MedicalOrderCategory.PRUEBAS_PSICOMETRICAS
] as const

// Directive categories don't need authorization — they're internal clinical instructions
// that take effect on creation (state ACTIVA).
export const DIRECTIVE_CATEGORIES: readonly MedicalOrderCategory[] = [
  MedicalOrderCategory.ORDENES_MEDICAS,
  MedicalOrderCategory.ACTIVIDAD_FISICA,
  MedicalOrderCategory.CUIDADOS_ESPECIALES,
  MedicalOrderCategory.DIETA,
  MedicalOrderCategory.RESTRICCIONES_MOVILIDAD,
  MedicalOrderCategory.PERMISOS_VISITA,
  MedicalOrderCategory.OTRAS
] as const

export function categoryRequiresAuthorization(category: MedicalOrderCategory): boolean {
  return !DIRECTIVE_CATEGORIES.includes(category)
}

// Terminal states reject all further transitions.
export const TERMINAL_STATES: readonly MedicalOrderStatus[] = [
  MedicalOrderStatus.NO_AUTORIZADO,
  MedicalOrderStatus.RESULTADOS_RECIBIDOS,
  MedicalOrderStatus.DESCONTINUADO
] as const

// Discontinue is only allowed from these states. Once an order moves to EN_PROCESO,
// the action has been initiated externally and can no longer be cancelled.
export const DISCONTINUABLE_STATES: readonly MedicalOrderStatus[] = [
  MedicalOrderStatus.ACTIVA,
  MedicalOrderStatus.SOLICITADO,
  MedicalOrderStatus.AUTORIZADO
] as const

// States from which a result document may be uploaded against a results-bearing order.
// Mirrors MedicalOrderDocumentService.UPLOADABLE_STATES on the backend.
export const RESULT_UPLOADABLE_STATES: readonly MedicalOrderStatus[] = [
  MedicalOrderStatus.AUTORIZADO,
  MedicalOrderStatus.EN_PROCESO,
  MedicalOrderStatus.RESULTADOS_RECIBIDOS
] as const

export function isTerminalStatus(status: MedicalOrderStatus): boolean {
  return TERMINAL_STATES.includes(status)
}

export function canDiscontinueStatus(status: MedicalOrderStatus): boolean {
  return DISCONTINUABLE_STATES.includes(status)
}

export function canUploadResultDocument(
  category: MedicalOrderCategory,
  status: MedicalOrderStatus
): boolean {
  return RESULTS_BEARING_CATEGORIES.includes(category) && RESULT_UPLOADABLE_STATES.includes(status)
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
  authorizedAt: string | null
  authorizedBy: MedicalStaffResponse | null
  inProgressAt: string | null
  inProgressBy: MedicalStaffResponse | null
  resultsReceivedAt: string | null
  resultsReceivedBy: MedicalStaffResponse | null
  rejectionReason: string | null
  emergencyAuthorized: boolean
  emergencyReason: EmergencyAuthorizationReason | null
  emergencyReasonNote: string | null
  emergencyAt: string | null
  emergencyBy: MedicalStaffResponse | null
  discontinuedAt: string | null
  discontinuedBy: MedicalStaffResponse | null
  inventoryItemId: number | null
  inventoryItemName: string | null
  documentCount: number
  createdAt: string | null
  updatedAt: string | null
  createdBy: MedicalStaffResponse | null
  updatedBy: MedicalStaffResponse | null
}

// Lightweight item used by the cross-admission orders-by-state dashboard.
export interface MedicalOrderListItemResponse {
  id: number
  admissionId: number
  patientId: number
  patientFirstName: string
  patientLastName: string
  category: MedicalOrderCategory
  status: MedicalOrderStatus
  startDate: string
  summary: string | null
  medication: string | null
  dosage: string | null
  createdAt: string | null
  createdBy: MedicalStaffResponse | null
  authorizedAt: string | null
  inProgressAt: string | null
  resultsReceivedAt: string | null
  discontinuedAt: string | null
  emergencyAuthorized: boolean
  documentCount: number
}

export interface RejectMedicalOrderRequest {
  reason?: string | null
}

export interface EmergencyAuthorizeMedicalOrderRequest {
  reason: EmergencyAuthorizationReason
  reasonNote?: string | null
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
  inventoryItemId?: number | null
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
  inventoryItemId?: number | null
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
