export enum Sex {
  MALE = 'MALE',
  FEMALE = 'FEMALE'
}

export enum MaritalStatus {
  SINGLE = 'SINGLE',
  MARRIED = 'MARRIED',
  DIVORCED = 'DIVORCED',
  WIDOWED = 'WIDOWED',
  SEPARATED = 'SEPARATED',
  OTHER = 'OTHER'
}

export enum EducationLevel {
  NONE = 'NONE',
  PRIMARY = 'PRIMARY',
  SECONDARY = 'SECONDARY',
  TECHNICAL = 'TECHNICAL',
  UNIVERSITY = 'UNIVERSITY',
  POSTGRADUATE = 'POSTGRADUATE'
}

export interface UserSummary {
  id: number
  username: string
  firstName: string | null
  lastName: string | null
}

export interface EmergencyContact {
  id?: number
  name: string
  relationship: string
  phone: string
}

export interface Patient {
  id: number
  firstName: string
  lastName: string
  age: number
  sex: Sex
  gender: string
  maritalStatus: MaritalStatus
  religion: string
  educationLevel: EducationLevel
  occupation: string
  address: string
  email: string
  idDocumentNumber: string | null
  notes: string | null
  hasIdDocument: boolean
  emergencyContacts: EmergencyContact[]
  createdAt: string | null
  createdBy: UserSummary | null
  updatedAt: string | null
  updatedBy: UserSummary | null
}

export interface PatientSummary {
  id: number
  firstName: string
  lastName: string
  age: number
  sex: Sex
  idDocumentNumber: string | null
  hasIdDocument: boolean
  hasActiveAdmission: boolean
}

export interface CreatePatientRequest {
  firstName: string
  lastName: string
  age: number
  sex: Sex
  gender: string
  maritalStatus: MaritalStatus
  religion: string
  educationLevel: EducationLevel
  occupation: string
  address: string
  email: string
  idDocumentNumber?: string | null
  notes?: string | null
  emergencyContacts: Omit<EmergencyContact, 'id'>[]
}

export interface UpdatePatientRequest {
  firstName: string
  lastName: string
  age: number
  sex: Sex
  gender: string
  maritalStatus: MaritalStatus
  religion: string
  educationLevel: EducationLevel
  occupation: string
  address: string
  email: string
  idDocumentNumber?: string | null
  notes?: string | null
  emergencyContacts: EmergencyContact[]
}

export interface DuplicatePatientResponse {
  success: false
  message: string
  data: {
    potentialDuplicates: PatientSummary[]
  }
}
