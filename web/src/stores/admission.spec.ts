import { describe, it, expect, beforeEach, vi, afterEach, type Mock } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useAdmissionStore } from './admission'
import api from '@/services/api'
import type { AdmissionDetail, AdmissionListItem, Doctor } from '@/types/admission'
import type { PatientSummary } from '@/types'
import { AdmissionStatus, AdmissionType } from '@/types/admission'
import { RoomType } from '@/types/room'

vi.mock('@/services/api', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn()
  }
}))

const mockedApi = api as unknown as {
  get: Mock
  post: Mock
  put: Mock
  delete: Mock
}

const mockPatient = {
  id: 1,
  firstName: 'Juan',
  lastName: 'Pérez García',
  age: 45,
  idDocumentNumber: '1234567890101',
  hasIdDocument: false,
  hasActiveAdmission: false
}

const mockTriageCode = {
  id: 1,
  code: 'A',
  color: '#FF0000',
  description: 'Critical'
}

const mockRoom = {
  id: 1,
  number: '101',
  type: RoomType.PRIVATE
}

const mockDoctor: Doctor = {
  id: 3,
  firstName: 'Dr. Maria',
  lastName: 'Garcia',
  salutation: 'Dr.',
  username: 'maria.garcia'
}

const mockAdmissionDetail: AdmissionDetail = {
  id: 1,
  patient: mockPatient,
  triageCode: mockTriageCode,
  room: mockRoom,
  treatingPhysician: mockDoctor,
  type: AdmissionType.HOSPITALIZATION,
  admissionDate: '2026-01-23T10:30:00',
  dischargeDate: null,
  status: AdmissionStatus.ACTIVE,
  inventory: 'Wallet, phone, glasses',
  hasConsentDocument: false,
  consultingPhysicians: [],
  createdAt: '2026-01-23T10:35:00',
  createdBy: { id: 2, username: 'receptionist', firstName: 'Reception', lastName: 'Staff' },
  updatedAt: '2026-01-23T10:35:00',
  updatedBy: { id: 2, username: 'receptionist', firstName: 'Reception', lastName: 'Staff' }
}

const mockAdmissionListItem: AdmissionListItem = {
  id: 1,
  patient: mockPatient,
  triageCode: mockTriageCode,
  room: mockRoom,
  treatingPhysician: mockDoctor,
  type: AdmissionType.HOSPITALIZATION,
  admissionDate: '2026-01-23T10:30:00',
  dischargeDate: null,
  status: AdmissionStatus.ACTIVE,
  hasConsentDocument: false,
  createdAt: '2026-01-23T10:35:00'
}

describe('useAdmissionStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  afterEach(() => {
    vi.resetAllMocks()
  })

  describe('initial state', () => {
    it('should start with empty state', () => {
      const store = useAdmissionStore()

      expect(store.admissions).toEqual([])
      expect(store.totalAdmissions).toBe(0)
      expect(store.currentAdmission).toBeNull()
      expect(store.doctors).toEqual([])
      expect(store.loading).toBe(false)
    })
  })

  describe('fetchAdmissions', () => {
    it('should fetch admissions and update state', async () => {
      mockedApi.get.mockResolvedValueOnce({
        data: {
          success: true,
          data: {
            content: [mockAdmissionListItem],
            page: { totalElements: 1, totalPages: 1, size: 20, number: 0 }
          }
        }
      })

      const store = useAdmissionStore()
      await store.fetchAdmissions(0, 20)

      expect(mockedApi.get).toHaveBeenCalledWith('/v1/admissions', {
        params: { page: 0, size: 20 }
      })
      expect(store.admissions).toHaveLength(1)
      expect(store.admissions[0]).toEqual(mockAdmissionListItem)
      expect(store.totalAdmissions).toBe(1)
    })

    it('should include status filter when provided', async () => {
      mockedApi.get.mockResolvedValueOnce({
        data: {
          success: true,
          data: {
            content: [mockAdmissionListItem],
            page: { totalElements: 1, totalPages: 1, size: 20, number: 0 }
          }
        }
      })

      const store = useAdmissionStore()
      await store.fetchAdmissions(0, 20, AdmissionStatus.ACTIVE)

      expect(mockedApi.get).toHaveBeenCalledWith('/v1/admissions', {
        params: { page: 0, size: 20, status: AdmissionStatus.ACTIVE }
      })
    })

    it('should handle loading state', async () => {
      let resolvePromise: (value: unknown) => void = () => {}
      const promise = new Promise(resolve => {
        resolvePromise = resolve
      })
      mockedApi.get.mockReturnValueOnce(promise as Promise<{ data: unknown }>)

      const store = useAdmissionStore()
      const fetchPromise = store.fetchAdmissions(0, 20)

      expect(store.loading).toBe(true)

      resolvePromise({
        data: {
          success: true,
          data: { content: [], page: { totalElements: 0 } }
        }
      })

      await fetchPromise
      expect(store.loading).toBe(false)
    })
  })

  describe('fetchAdmission', () => {
    it('should fetch a single admission and set as current', async () => {
      mockedApi.get.mockResolvedValueOnce({
        data: { success: true, data: mockAdmissionDetail }
      })

      const store = useAdmissionStore()
      const result = await store.fetchAdmission(1)

      expect(mockedApi.get).toHaveBeenCalledWith('/v1/admissions/1')
      expect(result).toEqual(mockAdmissionDetail)
      expect(store.currentAdmission).toEqual(mockAdmissionDetail)
    })

    it('should throw error when admission not found', async () => {
      mockedApi.get.mockResolvedValueOnce({
        data: { success: false, message: 'Admission not found' }
      })

      const store = useAdmissionStore()

      await expect(store.fetchAdmission(999)).rejects.toThrow('Admission not found')
    })
  })

  describe('createAdmission', () => {
    it('should create admission and return data', async () => {
      mockedApi.post.mockResolvedValueOnce({
        data: { success: true, data: mockAdmissionDetail }
      })

      const store = useAdmissionStore()
      const createData = {
        patientId: 1,
        triageCodeId: 1,
        roomId: 1,
        treatingPhysicianId: 3,
        type: AdmissionType.HOSPITALIZATION,
        admissionDate: '2026-01-23T10:30:00',
        inventory: 'Wallet, phone, glasses'
      }

      const result = await store.createAdmission(createData)

      expect(mockedApi.post).toHaveBeenCalledWith('/v1/admissions', createData)
      expect(result).toEqual(mockAdmissionDetail)
    })

    it('should throw error on failure', async () => {
      mockedApi.post.mockResolvedValueOnce({
        data: { success: false, message: 'Room is full' }
      })

      const store = useAdmissionStore()
      const createData = {
        patientId: 1,
        triageCodeId: 1,
        roomId: 1,
        treatingPhysicianId: 3,
        type: AdmissionType.HOSPITALIZATION,
        admissionDate: '2026-01-23T10:30:00'
      }

      await expect(store.createAdmission(createData)).rejects.toThrow('Room is full')
    })
  })

  describe('updateAdmission', () => {
    it('should update admission and update current state', async () => {
      const updatedAdmission = { ...mockAdmissionDetail, inventory: 'Updated inventory' }
      mockedApi.put.mockResolvedValueOnce({
        data: { success: true, data: updatedAdmission }
      })

      const store = useAdmissionStore()
      const updateData = {
        triageCodeId: 1,
        roomId: 1,
        treatingPhysicianId: 3,
        inventory: 'Updated inventory'
      }

      const result = await store.updateAdmission(1, updateData)

      expect(mockedApi.put).toHaveBeenCalledWith('/v1/admissions/1', updateData)
      expect(result.inventory).toBe('Updated inventory')
      expect(store.currentAdmission?.inventory).toBe('Updated inventory')
    })
  })

  describe('dischargePatient', () => {
    it('should discharge patient and update status', async () => {
      const dischargedAdmission = {
        ...mockAdmissionDetail,
        status: AdmissionStatus.DISCHARGED,
        dischargeDate: '2026-01-23T15:00:00'
      }
      mockedApi.post.mockResolvedValueOnce({
        data: { success: true, data: dischargedAdmission }
      })

      const store = useAdmissionStore()
      const result = await store.dischargePatient(1)

      expect(mockedApi.post).toHaveBeenCalledWith('/v1/admissions/1/discharge')
      expect(result.status).toBe(AdmissionStatus.DISCHARGED)
      expect(result.dischargeDate).toBe('2026-01-23T15:00:00')
      expect(store.currentAdmission?.status).toBe(AdmissionStatus.DISCHARGED)
    })

    it('should throw error if already discharged', async () => {
      mockedApi.post.mockResolvedValueOnce({
        data: { success: false, message: 'Patient is already discharged' }
      })

      const store = useAdmissionStore()

      await expect(store.dischargePatient(1)).rejects.toThrow('Patient is already discharged')
    })
  })

  describe('deleteAdmission', () => {
    it('should delete admission', async () => {
      mockedApi.delete.mockResolvedValueOnce({
        data: { success: true }
      })

      const store = useAdmissionStore()
      await store.deleteAdmission(1)

      expect(mockedApi.delete).toHaveBeenCalledWith('/v1/admissions/1')
    })

    it('should throw error on failure', async () => {
      mockedApi.delete.mockResolvedValueOnce({
        data: { success: false, message: 'Delete failed' }
      })

      const store = useAdmissionStore()

      await expect(store.deleteAdmission(1)).rejects.toThrow('Delete failed')
    })
  })

  describe('uploadConsentDocument', () => {
    it('should upload consent document and update current admission', async () => {
      const admissionWithConsent = { ...mockAdmissionDetail, hasConsentDocument: true }
      mockedApi.post.mockResolvedValueOnce({
        data: { success: true, data: admissionWithConsent }
      })

      const store = useAdmissionStore()
      const file = new File(['test content'], 'consent.pdf', { type: 'application/pdf' })

      const result = await store.uploadConsentDocument(1, file)

      expect(mockedApi.post).toHaveBeenCalledWith(
        '/v1/admissions/1/consent',
        expect.any(FormData),
        {
          headers: { 'Content-Type': 'multipart/form-data' }
        }
      )
      expect(result.hasConsentDocument).toBe(true)
      expect(store.currentAdmission?.hasConsentDocument).toBe(true)
    })
  })

  describe('downloadConsentDocument', () => {
    it('should download consent document as blob', async () => {
      const mockBlob = new Blob(['test'], { type: 'application/pdf' })
      mockedApi.get.mockResolvedValueOnce({ data: mockBlob })

      const store = useAdmissionStore()
      const result = await store.downloadConsentDocument(1)

      expect(mockedApi.get).toHaveBeenCalledWith('/v1/admissions/1/consent', {
        responseType: 'blob'
      })
      expect(result).toEqual(mockBlob)
    })
  })

  describe('searchPatients', () => {
    it('should search patients and return results', async () => {
      const mockPatients: PatientSummary[] = [
        {
          id: 1,
          firstName: 'Juan',
          lastName: 'Pérez',
          age: 45,
          idDocumentNumber: '123',
          hasIdDocument: false,
          hasActiveAdmission: false
        }
      ]
      mockedApi.get.mockResolvedValueOnce({
        data: { success: true, data: mockPatients }
      })

      const store = useAdmissionStore()
      const result = await store.searchPatients('Juan')

      expect(mockedApi.get).toHaveBeenCalledWith('/v1/admissions/patients/search', {
        params: { q: 'Juan' }
      })
      expect(result).toHaveLength(1)
      expect(result[0]!.firstName).toBe('Juan')
    })

    it('should return empty array for blank query', async () => {
      const store = useAdmissionStore()
      const result = await store.searchPatients('   ')

      expect(mockedApi.get).not.toHaveBeenCalled()
      expect(result).toEqual([])
    })

    it('should return empty array on error', async () => {
      mockedApi.get.mockRejectedValueOnce(new Error('Network error'))

      const store = useAdmissionStore()
      const result = await store.searchPatients('Juan')

      expect(result).toEqual([])
    })
  })

  describe('fetchDoctors', () => {
    it('should fetch doctors and update state', async () => {
      const mockDoctors: Doctor[] = [mockDoctor]
      mockedApi.get.mockResolvedValueOnce({
        data: { success: true, data: mockDoctors }
      })

      const store = useAdmissionStore()
      await store.fetchDoctors()

      expect(mockedApi.get).toHaveBeenCalledWith('/v1/admissions/doctors')
      expect(store.doctors).toHaveLength(1)
      expect(store.doctors[0]!.firstName).toBe('Dr. Maria')
    })

    it('should set empty array on error', async () => {
      mockedApi.get.mockRejectedValueOnce(new Error('Network error'))

      const store = useAdmissionStore()
      await store.fetchDoctors()

      expect(store.doctors).toEqual([])
    })
  })

  describe('clearCurrentAdmission', () => {
    it('should clear current admission', () => {
      const store = useAdmissionStore()
      store.currentAdmission = mockAdmissionDetail

      expect(store.currentAdmission).not.toBeNull()

      store.clearCurrentAdmission()

      expect(store.currentAdmission).toBeNull()
    })
  })
})
