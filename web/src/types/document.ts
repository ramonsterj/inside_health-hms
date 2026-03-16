import type { UserSummary } from './patient'
import type { MedicalStaffResponse } from './medicalRecord'

export interface DocumentType {
  id: number
  code: string
  name: string
  description: string | null
  displayOrder: number
  createdAt: string | null
  createdBy: UserSummary | null
  updatedAt: string | null
  updatedBy: UserSummary | null
}

export interface DocumentTypeSummary {
  id: number
  code: string
  name: string
}

export interface ThumbnailDocument {
  id: number
  displayName: string
  fileName: string
  contentType: string
  fileSize: number
  hasThumbnail: boolean
  thumbnailUrl: string | null
  downloadUrl: string | null
}

export interface AdmissionDocument extends ThumbnailDocument {
  documentType: DocumentTypeSummary
  createdAt: string | null
  createdBy: UserSummary | null
}

export interface CreateDocumentTypeRequest {
  code: string
  name: string
  description?: string | null
  displayOrder?: number
}

export interface UpdateDocumentTypeRequest {
  code: string
  name: string
  description?: string | null
  displayOrder?: number
}

export interface UploadDocumentRequest {
  file: File
  documentTypeId: number
  displayName?: string
}

export interface MedicalOrderDocument extends ThumbnailDocument {
  createdAt: string | null
  createdBy: MedicalStaffResponse | null
}
