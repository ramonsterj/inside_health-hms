import { defineStore } from 'pinia'
import { ref } from 'vue'
import api from '@/services/api'
import type { AdmissionDocument } from '@/types/document'
import type { ApiResponse } from '@/types'

export const useDocumentStore = defineStore('document', () => {
  // Documents by admission ID
  const documents = ref<Map<number, AdmissionDocument[]>>(new Map())
  const loading = ref(false)
  const uploading = ref(false)

  // Document viewer state
  const viewerDocument = ref<AdmissionDocument | null>(null)
  const viewerAdmissionId = ref<number | null>(null)

  async function fetchDocuments(admissionId: number): Promise<AdmissionDocument[]> {
    loading.value = true
    try {
      const response = await api.get<ApiResponse<AdmissionDocument[]>>(
        `/v1/admissions/${admissionId}/documents`
      )
      if (response.data.success && response.data.data) {
        documents.value.set(admissionId, response.data.data)
        return response.data.data
      }
      throw new Error(response.data.message || 'Failed to load documents')
    } finally {
      loading.value = false
    }
  }

  async function uploadDocument(
    admissionId: number,
    file: File,
    documentTypeId: number,
    displayName?: string
  ): Promise<AdmissionDocument> {
    uploading.value = true
    try {
      const formData = new FormData()
      formData.append('file', file)
      formData.append('documentTypeId', documentTypeId.toString())
      if (displayName) {
        formData.append('displayName', displayName)
      }

      const response = await api.post<ApiResponse<AdmissionDocument>>(
        `/v1/admissions/${admissionId}/documents`,
        formData,
        {
          headers: {
            'Content-Type': 'multipart/form-data'
          }
        }
      )

      if (response.data.success && response.data.data) {
        // Update local cache
        const existingDocs = documents.value.get(admissionId) || []
        documents.value.set(admissionId, [response.data.data, ...existingDocs])
        return response.data.data
      }
      throw new Error(response.data.message || 'Upload failed')
    } finally {
      uploading.value = false
    }
  }

  async function downloadDocument(admissionId: number, documentId: number): Promise<Blob> {
    const response = await api.get(`/v1/admissions/${admissionId}/documents/${documentId}/file`, {
      responseType: 'blob'
    })
    return response.data
  }

  async function getThumbnail(admissionId: number, documentId: number): Promise<Blob> {
    const response = await api.get(
      `/v1/admissions/${admissionId}/documents/${documentId}/thumbnail`,
      {
        responseType: 'blob'
      }
    )
    return response.data
  }

  async function deleteDocument(admissionId: number, documentId: number): Promise<void> {
    loading.value = true
    try {
      const response = await api.delete<ApiResponse<void>>(
        `/v1/admissions/${admissionId}/documents/${documentId}`
      )
      if (!response.data.success) {
        throw new Error(response.data.message || 'Delete failed')
      }

      // Update local cache
      const existingDocs = documents.value.get(admissionId) || []
      documents.value.set(
        admissionId,
        existingDocs.filter(doc => doc.id !== documentId)
      )
    } finally {
      loading.value = false
    }
  }

  function getDocuments(admissionId: number): AdmissionDocument[] {
    return documents.value.get(admissionId) || []
  }

  function setViewerDocument(admissionId: number, document: AdmissionDocument): void {
    viewerAdmissionId.value = admissionId
    viewerDocument.value = document
  }

  function clearViewerDocument(): void {
    viewerDocument.value = null
    viewerAdmissionId.value = null
  }

  function clearDocuments(admissionId: number): void {
    documents.value.delete(admissionId)
  }

  return {
    documents,
    loading,
    uploading,
    viewerDocument,
    viewerAdmissionId,
    fetchDocuments,
    uploadDocument,
    downloadDocument,
    getThumbnail,
    deleteDocument,
    getDocuments,
    setViewerDocument,
    clearViewerDocument,
    clearDocuments
  }
})
