import type { PatientSummary, UserSummary } from './patient'
import type { RoomSummary } from './room'
import type { TriageCodeSummary } from './triageCode'

export enum AdmissionStatus {
  ACTIVE = 'ACTIVE',
  DISCHARGED = 'DISCHARGED'
}

export enum AdmissionType {
  HOSPITALIZATION = 'HOSPITALIZATION',
  AMBULATORY = 'AMBULATORY',
  ELECTROSHOCK_THERAPY = 'ELECTROSHOCK_THERAPY',
  KETAMINE_INFUSION = 'KETAMINE_INFUSION',
  EMERGENCY = 'EMERGENCY'
}

export function admissionTypeRequiresRoom(type: AdmissionType): boolean {
  return type === AdmissionType.HOSPITALIZATION
}

export function admissionTypeRequiresTriageCode(type: AdmissionType): boolean {
  return type === AdmissionType.HOSPITALIZATION || type === AdmissionType.EMERGENCY
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
  triageCode: TriageCodeSummary | null
  room: RoomSummary | null
  treatingPhysician: Doctor
  admissionDate: string
  dischargeDate: string | null
  status: AdmissionStatus
  type: AdmissionType
  hasConsentDocument: boolean
  createdAt: string | null
}

export interface AdmissionDetail {
  id: number
  patient: PatientSummary
  triageCode: TriageCodeSummary | null
  room: RoomSummary | null
  treatingPhysician: Doctor
  admissionDate: string
  dischargeDate: string | null
  status: AdmissionStatus
  type: AdmissionType
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
  triageCodeId: number | null
  roomId: number | null
  treatingPhysicianId: number
  admissionDate: string
  type: AdmissionType
  inventory?: string | null
}

export interface UpdateAdmissionRequest {
  triageCodeId: number | null
  roomId: number | null
  treatingPhysicianId: number
  type?: AdmissionType
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
