import { defineStore } from 'pinia'
import { ref } from 'vue'
import api from '@/services/api'
import type {
  DocumentType,
  DocumentTypeSummary,
  CreateDocumentTypeRequest,
  UpdateDocumentTypeRequest
} from '@/types/document'
import type { ApiResponse } from '@/types'

export const useDocumentTypeStore = defineStore('documentType', () => {
  const documentTypes = ref<DocumentType[]>([])
  const documentTypeSummaries = ref<DocumentTypeSummary[]>([])
  const currentDocumentType = ref<DocumentType | null>(null)
  const loading = ref(false)

  async function fetchDocumentTypes(): Promise<void> {
    loading.value = true
    try {
      const response = await api.get<ApiResponse<DocumentType[]>>('/v1/document-types')
      if (response.data.success && response.data.data) {
        documentTypes.value = response.data.data
      } else {
        throw new Error(response.data.message || 'Failed to load document types')
      }
    } finally {
      loading.value = false
    }
  }

  async function fetchDocumentTypeSummaries(): Promise<void> {
    loading.value = true
    try {
      const response = await api.get<ApiResponse<DocumentTypeSummary[]>>(
        '/v1/document-types/summary'
      )
      if (response.data.success && response.data.data) {
        documentTypeSummaries.value = response.data.data
      } else {
        throw new Error(response.data.message || 'Failed to load document types')
      }
    } finally {
      loading.value = false
    }
  }

  async function fetchDocumentType(id: number): Promise<DocumentType> {
    loading.value = true
    try {
      const response = await api.get<ApiResponse<DocumentType>>(`/v1/document-types/${id}`)
      if (response.data.success && response.data.data) {
        currentDocumentType.value = response.data.data
        return response.data.data
      }
      throw new Error(response.data.message || 'Document type not found')
    } finally {
      loading.value = false
    }
  }

  async function createDocumentType(data: CreateDocumentTypeRequest): Promise<DocumentType> {
    loading.value = true
    try {
      const response = await api.post<ApiResponse<DocumentType>>('/v1/document-types', data)
      if (response.data.success && response.data.data) {
        return response.data.data
      }
      throw new Error(response.data.message || 'Create document type failed')
    } finally {
      loading.value = false
    }
  }

  async function updateDocumentType(
    id: number,
    data: UpdateDocumentTypeRequest
  ): Promise<DocumentType> {
    loading.value = true
    try {
      const response = await api.put<ApiResponse<DocumentType>>(`/v1/document-types/${id}`, data)
      if (response.data.success && response.data.data) {
        currentDocumentType.value = response.data.data
        return response.data.data
      }
      throw new Error(response.data.message || 'Update document type failed')
    } finally {
      loading.value = false
    }
  }

  async function deleteDocumentType(id: number): Promise<void> {
    loading.value = true
    try {
      const response = await api.delete<ApiResponse<void>>(`/v1/document-types/${id}`)
      if (!response.data.success) {
        throw new Error(response.data.message || 'Delete document type failed')
      }
    } finally {
      loading.value = false
    }
  }

  function clearCurrentDocumentType(): void {
    currentDocumentType.value = null
  }

  return {
    documentTypes,
    documentTypeSummaries,
    currentDocumentType,
    loading,
    fetchDocumentTypes,
    fetchDocumentTypeSummaries,
    fetchDocumentType,
    createDocumentType,
    updateDocumentType,
    deleteDocumentType,
    clearCurrentDocumentType
  }
})
