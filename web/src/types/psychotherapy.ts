import type { MedicalStaffResponse } from './medicalRecord'

// Psychotherapy Category types
export interface PsychotherapyCategory {
  id: number
  name: string
  description: string | null
  displayOrder: number
  active: boolean
  price: number | null
  cost: number | null
  createdAt: string | null
  updatedAt: string | null
}

export interface PsychotherapyCategorySummary {
  id: number
  name: string
}

export interface CreatePsychotherapyCategoryRequest {
  name: string
  description?: string | null
  displayOrder?: number
  active?: boolean
  price?: number | null
  cost?: number | null
}

export interface UpdatePsychotherapyCategoryRequest {
  name: string
  description?: string | null
  displayOrder?: number
  active?: boolean
  price?: number | null
  cost?: number | null
}

// Psychotherapy Activity types
export interface PsychotherapyActivity {
  id: number
  admissionId: number
  category: PsychotherapyCategorySummary
  description: string
  createdAt: string | null
  createdBy: MedicalStaffResponse | null
}

export interface CreatePsychotherapyActivityRequest {
  categoryId: number
  description: string
}
