import type { PatientSummary, UserSummary } from './patient'
import type { RoomSummary } from './room'
import type { TriageCodeSummary } from './triageCode'

export enum AdmissionStatus {
  ACTIVE = 'ACTIVE',
  DISCHARGED = 'DISCHARGED'
}

export interface Doctor {
  id: number
  salutation: string | null
  firstName: string | null
  lastName: string | null
  username: string
}

export interface AdmissionListItem {
  id: number
  patient: PatientSummary
  triageCode: TriageCodeSummary
  room: RoomSummary
  treatingPhysician: Doctor
  admissionDate: string
  dischargeDate: string | null
  status: AdmissionStatus
  hasConsentDocument: boolean
  createdAt: string | null
}

export interface AdmissionDetail {
  id: number
  patient: PatientSummary
  triageCode: TriageCodeSummary
  room: RoomSummary
  treatingPhysician: Doctor
  admissionDate: string
  dischargeDate: string | null
  status: AdmissionStatus
  inventory: string | null
  hasConsentDocument: boolean
  consultingPhysicians: ConsultingPhysician[]
  createdAt: string | null
  createdBy: UserSummary | null
  updatedAt: string | null
  updatedBy: UserSummary | null
}

export interface CreateAdmissionRequest {
  patientId: number
  triageCodeId: number
  roomId: number
  treatingPhysicianId: number
  admissionDate: string
  inventory?: string | null
}

export interface UpdateAdmissionRequest {
  triageCodeId: number
  roomId: number
  treatingPhysicianId: number
  inventory?: string | null
}

export interface ConsultingPhysician {
  id: number
  physician: Doctor
  reason: string | null
  requestedDate: string | null
  createdAt: string | null
  createdBy: UserSummary | null
}

export interface AddConsultingPhysicianRequest {
  physicianId: number
  reason?: string | null
  requestedDate?: string | null
}
