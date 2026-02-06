/**
 * Nursing Module types for Nursing Notes and Vital Signs.
 * Matches backend DTOs from the nursing module feature.
 */

import type { MedicalStaffResponse } from './medicalRecord'

// Nursing Note Response
export interface NursingNoteResponse {
  id: number
  admissionId: number
  description: string
  createdAt: string
  updatedAt: string
  createdBy: MedicalStaffResponse | null
  updatedBy: MedicalStaffResponse | null
  canEdit: boolean
}

export interface CreateNursingNoteRequest {
  description: string
}

export interface UpdateNursingNoteRequest {
  description: string
}

// Vital Sign Response
export interface VitalSignResponse {
  id: number
  admissionId: number
  recordedAt: string
  systolicBp: number
  diastolicBp: number
  heartRate: number
  respiratoryRate: number
  temperature: number
  oxygenSaturation: number
  other: string | null
  createdAt: string
  updatedAt: string
  createdBy: MedicalStaffResponse | null
  updatedBy: MedicalStaffResponse | null
  canEdit: boolean
}

export interface CreateVitalSignRequest {
  recordedAt?: string | null
  systolicBp: number
  diastolicBp: number
  heartRate: number
  respiratoryRate: number
  temperature: number
  oxygenSaturation: number
  other?: string | null
}

export interface UpdateVitalSignRequest {
  recordedAt?: string | null
  systolicBp: number
  diastolicBp: number
  heartRate: number
  respiratoryRate: number
  temperature: number
  oxygenSaturation: number
  other?: string | null
}

// Date range filter for vital signs
export interface VitalSignDateRange {
  fromDate: string | null
  toDate: string | null
}
