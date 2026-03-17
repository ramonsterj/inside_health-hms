import { defineStore } from 'pinia'
import { ref } from 'vue'
import api from '@/services/api'
import type { MedicalOrderDocument } from '@/types/document'
import type { ApiResponse } from '@/types'

export const useMedicalOrderDocumentStore = defineStore('medicalOrderDocument', () => {
  // Documents by order ID
  const documents = ref<Map<number, MedicalOrderDocument[]>>(new Map())
  const loading = ref(false)
  const uploading = ref(false)

  // Document viewer state
  const viewerDocument = ref<MedicalOrderDocument | null>(null)
  const viewerAdmissionId = ref<number | null>(null)
  const viewerOrderId = ref<number | null>(null)

  function basePath(admissionId: number, orderId: number): string {
    return `/v1/admissions/${admissionId}/medical-orders/${orderId}/documents`
  }

  async function fetchDocuments(
    admissionId: number,
    orderId: number
  ): Promise<MedicalOrderDocument[]> {
    loading.value = true
    try {
      const response = await api.get<ApiResponse<MedicalOrderDocument[]>>(
        basePath(admissionId, orderId)
      )
      if (response.data.success && response.data.data) {
        documents.value.set(orderId, response.data.data)
        return response.data.data
      }
      throw new Error(response.data.message || 'Failed to load documents')
    } finally {
      loading.value = false
    }
  }

  async function uploadDocument(
    admissionId: number,
    orderId: number,
    file: File,
    displayName?: string
  ): Promise<MedicalOrderDocument> {
    uploading.value = true
    try {
      const formData = new FormData()
      formData.append('file', file)
      if (displayName) {
        formData.append('displayName', displayName)
      }

      const response = await api.post<ApiResponse<MedicalOrderDocument>>(
        basePath(admissionId, orderId),
        formData,
        {
          headers: {
            'Content-Type': 'multipart/form-data'
          }
        }
      )

      if (response.data.success && response.data.data) {
        // Update local cache
        const existingDocs = documents.value.get(orderId) || []
        documents.value.set(orderId, [response.data.data, ...existingDocs])
        return response.data.data
      }
      throw new Error(response.data.message || 'Upload failed')
    } finally {
      uploading.value = false
    }
  }

  async function downloadDocument(
    admissionId: number,
    orderId: number,
    documentId: number
  ): Promise<Blob> {
    const response = await api.get(`${basePath(admissionId, orderId)}/${documentId}/file`, {
      responseType: 'blob'
    })
    return response.data
  }

  async function getThumbnail(
    admissionId: number,
    orderId: number,
    documentId: number
  ): Promise<Blob> {
    const response = await api.get(`${basePath(admissionId, orderId)}/${documentId}/thumbnail`, {
      responseType: 'blob'
    })
    return response.data
  }

  async function deleteDocument(
    admissionId: number,
    orderId: number,
    documentId: number
  ): Promise<void> {
    loading.value = true
    try {
      const response = await api.delete<ApiResponse<void>>(
        `${basePath(admissionId, orderId)}/${documentId}`
      )
      if (!response.data.success) {
        throw new Error(response.data.message || 'Delete failed')
      }

      // Update local cache
      const existingDocs = documents.value.get(orderId) || []
      documents.value.set(
        orderId,
        existingDocs.filter(doc => doc.id !== documentId)
      )
    } finally {
      loading.value = false
    }
  }

  function getDocuments(orderId: number): MedicalOrderDocument[] {
    return documents.value.get(orderId) || []
  }

  function setViewerDocument(
    admissionId: number,
    orderId: number,
    document: MedicalOrderDocument
  ): void {
    viewerAdmissionId.value = admissionId
    viewerOrderId.value = orderId
    viewerDocument.value = document
  }

  function clearViewerDocument(): void {
    viewerDocument.value = null
    viewerAdmissionId.value = null
    viewerOrderId.value = null
  }

  function clearDocuments(orderId: number): void {
    documents.value.delete(orderId)
  }

  return {
    documents,
    loading,
    uploading,
    viewerDocument,
    viewerAdmissionId,
    viewerOrderId,
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
