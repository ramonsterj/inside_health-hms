import { describe, it, expect, beforeEach, vi, afterEach, type Mock } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useMedicalOrderDocumentStore } from './medicalOrderDocument'
import api from '@/services/api'
import type { MedicalOrderDocument } from '@/types/document'

vi.mock('@/services/api', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    delete: vi.fn()
  }
}))

const mockedApi = api as unknown as {
  get: Mock
  post: Mock
  delete: Mock
}

const mockDocument: MedicalOrderDocument = {
  id: 1,
  displayName: 'Blood Work Results',
  fileName: 'lab-result.pdf',
  contentType: 'application/pdf',
  fileSize: 102400,
  hasThumbnail: false,
  thumbnailUrl: null,
  downloadUrl: '/v1/admissions/100/medical-orders/10/documents/1/file',
  createdAt: '2026-02-10T14:30:00Z',
  createdBy: {
    id: 2,
    salutation: 'DR',
    firstName: 'Maria',
    lastName: 'Garcia',
    roles: ['DOCTOR']
  }
}

const mockDocuments: MedicalOrderDocument[] = [
  mockDocument,
  {
    id: 2,
    displayName: 'X-Ray Scan',
    fileName: 'xray.jpg',
    contentType: 'image/jpeg',
    fileSize: 204800,
    hasThumbnail: true,
    thumbnailUrl: '/v1/admissions/100/medical-orders/10/documents/2/thumbnail',
    downloadUrl: '/v1/admissions/100/medical-orders/10/documents/2/file',
    createdAt: '2026-02-10T15:00:00Z',
    createdBy: {
      id: 3,
      salutation: null,
      firstName: 'Ana',
      lastName: 'Lopez',
      roles: ['NURSE']
    }
  }
]

describe('useMedicalOrderDocumentStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  afterEach(() => {
    vi.resetAllMocks()
  })

  describe('initial state', () => {
    it('should start with empty state', () => {
      const store = useMedicalOrderDocumentStore()

      expect(store.documents.size).toBe(0)
      expect(store.loading).toBe(false)
      expect(store.uploading).toBe(false)
      expect(store.viewerDocument).toBeNull()
      expect(store.viewerAdmissionId).toBeNull()
      expect(store.viewerOrderId).toBeNull()
    })
  })

  describe('fetchDocuments', () => {
    it('should fetch documents for an order', async () => {
      mockedApi.get.mockResolvedValueOnce({
        data: { success: true, data: mockDocuments }
      })

      const store = useMedicalOrderDocumentStore()
      const result = await store.fetchDocuments(100, 10)

      expect(mockedApi.get).toHaveBeenCalledWith('/v1/admissions/100/medical-orders/10/documents')
      expect(result).toHaveLength(2)
      expect(store.getDocuments(10)).toHaveLength(2)
    })

    it('should handle loading state during fetch', async () => {
      let resolvePromise: (value: unknown) => void = () => {}
      const promise = new Promise(resolve => {
        resolvePromise = resolve
      })

      mockedApi.get.mockReturnValueOnce(promise)

      const store = useMedicalOrderDocumentStore()
      const fetchPromise = store.fetchDocuments(100, 10)

      expect(store.loading).toBe(true)

      resolvePromise({
        data: { success: true, data: mockDocuments }
      })

      await fetchPromise
      expect(store.loading).toBe(false)
    })

    it('should cache results by orderId', async () => {
      mockedApi.get.mockResolvedValueOnce({
        data: { success: true, data: mockDocuments }
      })

      const store = useMedicalOrderDocumentStore()
      await store.fetchDocuments(100, 10)

      expect(store.getDocuments(10)).toHaveLength(2)
      expect(store.getDocuments(999)).toEqual([])
    })

    it('should throw error on API failure', async () => {
      mockedApi.get.mockResolvedValueOnce({
        data: { success: false, message: 'Order not found' }
      })

      const store = useMedicalOrderDocumentStore()

      await expect(store.fetchDocuments(100, 999)).rejects.toThrow('Order not found')
    })
  })

  describe('uploadDocument', () => {
    it('should upload document and update local cache', async () => {
      mockedApi.post.mockResolvedValueOnce({
        data: { success: true, data: mockDocument }
      })

      const store = useMedicalOrderDocumentStore()
      const file = new File(['content'], 'lab.pdf', { type: 'application/pdf' })
      const result = await store.uploadDocument(100, 10, file, 'Blood Work Results')

      expect(mockedApi.post).toHaveBeenCalledWith(
        '/v1/admissions/100/medical-orders/10/documents',
        expect.any(FormData),
        { headers: { 'Content-Type': 'multipart/form-data' } }
      )
      expect(result.displayName).toBe('Blood Work Results')
      expect(store.getDocuments(10)).toHaveLength(1)
    })

    it('should upload without display name', async () => {
      mockedApi.post.mockResolvedValueOnce({
        data: { success: true, data: mockDocument }
      })

      const store = useMedicalOrderDocumentStore()
      const file = new File(['content'], 'lab.pdf', { type: 'application/pdf' })
      await store.uploadDocument(100, 10, file)

      const formData = mockedApi.post.mock.calls[0]![1] as FormData
      expect(formData.get('file')).toBeTruthy()
      expect(formData.get('displayName')).toBeNull()
    })

    it('should handle uploading state', async () => {
      let resolvePromise: (value: unknown) => void = () => {}
      const promise = new Promise(resolve => {
        resolvePromise = resolve
      })

      mockedApi.post.mockReturnValueOnce(promise)

      const store = useMedicalOrderDocumentStore()
      const file = new File(['content'], 'lab.pdf', { type: 'application/pdf' })
      const uploadPromise = store.uploadDocument(100, 10, file)

      expect(store.uploading).toBe(true)

      resolvePromise({
        data: { success: true, data: mockDocument }
      })

      await uploadPromise
      expect(store.uploading).toBe(false)
    })

    it('should throw error on upload failure', async () => {
      mockedApi.post.mockResolvedValueOnce({
        data: { success: false, message: 'Invalid file type' }
      })

      const store = useMedicalOrderDocumentStore()
      const file = new File(['content'], 'test.txt', { type: 'text/plain' })

      await expect(store.uploadDocument(100, 10, file)).rejects.toThrow('Invalid file type')
    })

    it('should prepend new document to existing cache', async () => {
      // Pre-populate cache
      mockedApi.get.mockResolvedValueOnce({
        data: { success: true, data: [mockDocuments[1]] }
      })

      const store = useMedicalOrderDocumentStore()
      await store.fetchDocuments(100, 10)
      expect(store.getDocuments(10)).toHaveLength(1)

      // Upload new document
      mockedApi.post.mockResolvedValueOnce({
        data: { success: true, data: mockDocument }
      })

      const file = new File(['content'], 'lab.pdf', { type: 'application/pdf' })
      await store.uploadDocument(100, 10, file)

      // Should have both documents, new one first
      const docs = store.getDocuments(10)
      expect(docs).toHaveLength(2)
      expect(docs[0]!.id).toBe(mockDocument.id)
    })
  })

  describe('downloadDocument', () => {
    it('should download document as blob', async () => {
      const mockBlob = new Blob(['content'], { type: 'application/pdf' })
      mockedApi.get.mockResolvedValueOnce({ data: mockBlob })

      const store = useMedicalOrderDocumentStore()
      const result = await store.downloadDocument(100, 10, 1)

      expect(mockedApi.get).toHaveBeenCalledWith(
        '/v1/admissions/100/medical-orders/10/documents/1/file',
        { responseType: 'blob' }
      )
      expect(result).toBeInstanceOf(Blob)
    })
  })

  describe('getThumbnail', () => {
    it('should fetch thumbnail as blob', async () => {
      const mockBlob = new Blob(['image'], { type: 'image/png' })
      mockedApi.get.mockResolvedValueOnce({ data: mockBlob })

      const store = useMedicalOrderDocumentStore()
      const result = await store.getThumbnail(100, 10, 2)

      expect(mockedApi.get).toHaveBeenCalledWith(
        '/v1/admissions/100/medical-orders/10/documents/2/thumbnail',
        { responseType: 'blob' }
      )
      expect(result).toBeInstanceOf(Blob)
    })
  })

  describe('deleteDocument', () => {
    it('should delete document and update local cache', async () => {
      // Pre-populate cache
      mockedApi.get.mockResolvedValueOnce({
        data: { success: true, data: mockDocuments }
      })

      const store = useMedicalOrderDocumentStore()
      await store.fetchDocuments(100, 10)
      expect(store.getDocuments(10)).toHaveLength(2)

      // Delete first document
      mockedApi.delete.mockResolvedValueOnce({
        data: { success: true, message: 'Deleted' }
      })

      await store.deleteDocument(100, 10, 1)

      expect(mockedApi.delete).toHaveBeenCalledWith(
        '/v1/admissions/100/medical-orders/10/documents/1'
      )
      expect(store.getDocuments(10)).toHaveLength(1)
      expect(store.getDocuments(10)[0]!.id).toBe(2)
    })

    it('should throw error on delete failure', async () => {
      mockedApi.delete.mockResolvedValueOnce({
        data: { success: false, message: 'Permission denied' }
      })

      const store = useMedicalOrderDocumentStore()

      await expect(store.deleteDocument(100, 10, 1)).rejects.toThrow('Permission denied')
    })
  })

  describe('viewer state', () => {
    it('should set viewer document', () => {
      const store = useMedicalOrderDocumentStore()

      store.setViewerDocument(100, 10, mockDocument)

      expect(store.viewerDocument).toEqual(mockDocument)
      expect(store.viewerAdmissionId).toBe(100)
      expect(store.viewerOrderId).toBe(10)
    })

    it('should clear viewer document', () => {
      const store = useMedicalOrderDocumentStore()

      store.setViewerDocument(100, 10, mockDocument)
      store.clearViewerDocument()

      expect(store.viewerDocument).toBeNull()
      expect(store.viewerAdmissionId).toBeNull()
      expect(store.viewerOrderId).toBeNull()
    })
  })

  describe('clearDocuments', () => {
    it('should clear documents for specific order', async () => {
      mockedApi.get.mockResolvedValueOnce({
        data: { success: true, data: mockDocuments }
      })

      const store = useMedicalOrderDocumentStore()
      await store.fetchDocuments(100, 10)
      expect(store.getDocuments(10)).toHaveLength(2)

      store.clearDocuments(10)
      expect(store.getDocuments(10)).toEqual([])
    })
  })

  describe('getDocuments', () => {
    it('should return empty array for unfetched order', () => {
      const store = useMedicalOrderDocumentStore()
      expect(store.getDocuments(999)).toEqual([])
    })
  })
})
