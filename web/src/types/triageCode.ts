import type { UserSummary } from './patient'

export interface TriageCode {
  id: number
  code: string
  color: string
  description: string | null
  displayOrder: number
  createdAt: string | null
  createdBy: UserSummary | null
  updatedAt: string | null
  updatedBy: UserSummary | null
}

export interface TriageCodeSummary {
  id: number
  code: string
  color: string
  description: string | null
}

export interface CreateTriageCodeRequest {
  code: string
  color: string
  description?: string | null
  displayOrder?: number
}

export interface UpdateTriageCodeRequest {
  code: string
  color: string
  description?: string | null
  displayOrder?: number
}
