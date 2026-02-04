import type { UserSummary } from './patient'

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

export interface AdmissionDocument {
  id: number
  documentType: DocumentTypeSummary
  displayName: string
  fileName: string
  contentType: string
  fileSize: number
  hasThumbnail: boolean
  thumbnailUrl: string | null
  downloadUrl: string | null
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
