import { describe, it, expect, beforeEach, vi, afterEach, type Mock } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { usePatientStore, DuplicatePatientError } from './patient'
import api from '@/services/api'
import type { Patient, PatientSummary } from '@/types'
import { Sex, MaritalStatus, EducationLevel } from '@/types'

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

const mockPatient: Patient = {
  id: 1,
  firstName: 'Juan',
  lastName: 'Pérez García',
  age: 45,
  sex: Sex.MALE,
  gender: 'Masculino',
  maritalStatus: MaritalStatus.MARRIED,
  religion: 'Católica',
  educationLevel: EducationLevel.UNIVERSITY,
  occupation: 'Ingeniero',
  address: '4a Calle 5-67 Zona 1, Guatemala',
  email: 'juan.perez@email.com',
  idDocumentNumber: '1234567890101',
  notes: 'Paciente referido',
  hasIdDocument: false,
  emergencyContacts: [
    { id: 1, name: 'María de Pérez', relationship: 'Esposa', phone: '+502 5555-1234' }
  ],
  createdAt: '2026-01-21T10:00:00Z',
  createdBy: { id: 5, username: 'receptionist1', firstName: 'Ana', lastName: 'García' },
  updatedAt: '2026-01-21T10:00:00Z',
  updatedBy: { id: 5, username: 'receptionist1', firstName: 'Ana', lastName: 'García' }
}

const mockPatientSummary: PatientSummary = {
  id: 1,
  firstName: 'Juan',
  lastName: 'Pérez García',
  age: 45,
  idDocumentNumber: '1234567890101',
  hasIdDocument: false
}

describe('usePatientStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  afterEach(() => {
    vi.resetAllMocks()
  })

  describe('initial state', () => {
    it('should start with empty state', () => {
      const store = usePatientStore()

      expect(store.patients).toEqual([])
      expect(store.totalPatients).toBe(0)
      expect(store.currentPatient).toBeNull()
      expect(store.loading).toBe(false)
    })
  })

  describe('fetchPatients', () => {
    it('should fetch patients and update state', async () => {
      mockedApi.get.mockResolvedValueOnce({
        data: {
          success: true,
          data: {
            content: [mockPatientSummary],
            page: { totalElements: 1, totalPages: 1, size: 20, number: 0 }
          }
        }
      })

      const store = usePatientStore()
      await store.fetchPatients(0, 20)

      expect(mockedApi.get).toHaveBeenCalledWith('/v1/patients', {
        params: { page: 0, size: 20 }
      })
      expect(store.patients).toHaveLength(1)
      expect(store.patients[0]).toEqual(mockPatientSummary)
      expect(store.totalPatients).toBe(1)
    })

    it('should include search parameter when provided', async () => {
      mockedApi.get.mockResolvedValueOnce({
        data: {
          success: true,
          data: {
            content: [mockPatientSummary],
            page: { totalElements: 1, totalPages: 1, size: 20, number: 0 }
          }
        }
      })

      const store = usePatientStore()
      await store.fetchPatients(0, 20, 'Juan')

      expect(mockedApi.get).toHaveBeenCalledWith('/v1/patients', {
        params: { page: 0, size: 20, search: 'Juan' }
      })
    })

    it('should handle loading state', async () => {
      let resolvePromise: (value: unknown) => void = () => {}
      const promise = new Promise(resolve => {
        resolvePromise = resolve
      })
      mockedApi.get.mockReturnValueOnce(promise as Promise<{ data: unknown }>)

      const store = usePatientStore()
      const fetchPromise = store.fetchPatients(0, 20)

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

  describe('fetchPatient', () => {
    it('should fetch a single patient and set as current', async () => {
      mockedApi.get.mockResolvedValueOnce({
        data: { success: true, data: mockPatient }
      })

      const store = usePatientStore()
      const result = await store.fetchPatient(1)

      expect(mockedApi.get).toHaveBeenCalledWith('/v1/patients/1')
      expect(result).toEqual(mockPatient)
      expect(store.currentPatient).toEqual(mockPatient)
    })

    it('should throw error when patient not found', async () => {
      mockedApi.get.mockResolvedValueOnce({
        data: { success: false, message: 'Patient not found' }
      })

      const store = usePatientStore()

      await expect(store.fetchPatient(999)).rejects.toThrow('Patient not found')
    })
  })

  describe('createPatient', () => {
    it('should create patient and return data', async () => {
      mockedApi.post.mockResolvedValueOnce({
        data: { success: true, data: mockPatient }
      })

      const store = usePatientStore()
      const createData = {
        firstName: 'Juan',
        lastName: 'Pérez García',
        age: 45,
        sex: Sex.MALE,
        gender: 'Masculino',
        maritalStatus: MaritalStatus.MARRIED,
        religion: 'Católica',
        educationLevel: EducationLevel.UNIVERSITY,
        occupation: 'Ingeniero',
        address: '4a Calle 5-67 Zona 1, Guatemala',
        email: 'juan.perez@email.com',
        emergencyContacts: [
          { name: 'María de Pérez', relationship: 'Esposa', phone: '+502 5555-1234' }
        ]
      }

      const result = await store.createPatient(createData)

      expect(mockedApi.post).toHaveBeenCalledWith('/v1/patients', createData)
      expect(result).toEqual(mockPatient)
    })

    it('should throw DuplicatePatientError on 409 conflict', async () => {
      const duplicateError = {
        response: {
          status: 409,
          data: {
            success: false,
            message: 'Potential duplicate patient found',
            data: {
              potentialDuplicates: [mockPatientSummary]
            }
          }
        }
      }
      mockedApi.post.mockRejectedValueOnce(duplicateError)

      const store = usePatientStore()
      const createData = {
        firstName: 'Juan',
        lastName: 'Pérez García',
        age: 45,
        sex: Sex.MALE,
        gender: 'Masculino',
        maritalStatus: MaritalStatus.MARRIED,
        religion: 'Católica',
        educationLevel: EducationLevel.UNIVERSITY,
        occupation: 'Ingeniero',
        address: '4a Calle 5-67 Zona 1, Guatemala',
        email: 'juan.perez@email.com',
        emergencyContacts: []
      }

      try {
        await store.createPatient(createData)
        expect.fail('Should have thrown DuplicatePatientError')
      } catch (error) {
        expect(error).toBeInstanceOf(DuplicatePatientError)
        const dupError = error as DuplicatePatientError
        expect(dupError.potentialDuplicates).toHaveLength(1)
        expect(dupError.potentialDuplicates[0]!.firstName).toBe('Juan')
      }
    })
  })

  describe('updatePatient', () => {
    it('should update patient and update current state', async () => {
      const updatedPatient = { ...mockPatient, firstName: 'Juan Carlos', age: 46 }
      mockedApi.put.mockResolvedValueOnce({
        data: { success: true, data: updatedPatient }
      })

      const store = usePatientStore()
      const updateData = {
        firstName: 'Juan Carlos',
        lastName: 'Pérez García',
        age: 46,
        sex: Sex.MALE,
        gender: 'Masculino',
        maritalStatus: MaritalStatus.MARRIED,
        religion: 'Católica',
        educationLevel: EducationLevel.UNIVERSITY,
        occupation: 'Ingeniero',
        address: '4a Calle 5-67 Zona 1, Guatemala',
        email: 'juan.perez@email.com',
        emergencyContacts: [
          { id: 1, name: 'María de Pérez', relationship: 'Esposa', phone: '+502 5555-1234' }
        ]
      }

      const result = await store.updatePatient(1, updateData)

      expect(mockedApi.put).toHaveBeenCalledWith('/v1/patients/1', updateData)
      expect(result.firstName).toBe('Juan Carlos')
      expect(result.age).toBe(46)
      expect(store.currentPatient?.firstName).toBe('Juan Carlos')
    })

    it('should throw DuplicatePatientError on 409 conflict during update', async () => {
      const duplicateError = {
        response: {
          status: 409,
          data: {
            success: false,
            message: 'Potential duplicate patient found',
            data: {
              potentialDuplicates: [mockPatientSummary]
            }
          }
        }
      }
      mockedApi.put.mockRejectedValueOnce(duplicateError)

      const store = usePatientStore()
      const updateData = {
        firstName: 'Juan',
        lastName: 'Pérez García',
        age: 45,
        sex: Sex.MALE,
        gender: 'Masculino',
        maritalStatus: MaritalStatus.MARRIED,
        religion: 'Católica',
        educationLevel: EducationLevel.UNIVERSITY,
        occupation: 'Ingeniero',
        address: '4a Calle 5-67 Zona 1, Guatemala',
        email: 'juan.perez@email.com',
        emergencyContacts: []
      }

      await expect(store.updatePatient(1, updateData)).rejects.toBeInstanceOf(DuplicatePatientError)
    })
  })

  describe('uploadIdDocument', () => {
    it('should upload ID document and update current patient', async () => {
      const patientWithDoc = { ...mockPatient, hasIdDocument: true }
      mockedApi.post.mockResolvedValueOnce({
        data: { success: true, data: patientWithDoc }
      })

      const store = usePatientStore()
      const file = new File(['test content'], 'id-document.jpg', { type: 'image/jpeg' })

      const result = await store.uploadIdDocument(1, file)

      expect(mockedApi.post).toHaveBeenCalledWith(
        '/v1/patients/1/id-document',
        expect.any(FormData),
        { headers: { 'Content-Type': 'multipart/form-data' } }
      )
      expect(result.hasIdDocument).toBe(true)
      expect(store.currentPatient?.hasIdDocument).toBe(true)
    })
  })

  describe('downloadIdDocument', () => {
    it('should download ID document as blob', async () => {
      const mockBlob = new Blob(['test'], { type: 'image/jpeg' })
      mockedApi.get.mockResolvedValueOnce({ data: mockBlob })

      const store = usePatientStore()
      const result = await store.downloadIdDocument(1)

      expect(mockedApi.get).toHaveBeenCalledWith('/v1/patients/1/id-document', {
        responseType: 'blob'
      })
      expect(result).toEqual(mockBlob)
    })
  })

  describe('deleteIdDocument', () => {
    it('should delete ID document and update current patient', async () => {
      const patientWithoutDoc = { ...mockPatient, hasIdDocument: false }
      mockedApi.delete.mockResolvedValueOnce({
        data: { success: true, data: patientWithoutDoc }
      })

      const store = usePatientStore()
      store.currentPatient = { ...mockPatient, hasIdDocument: true }

      const result = await store.deleteIdDocument(1)

      expect(mockedApi.delete).toHaveBeenCalledWith('/v1/patients/1/id-document')
      expect(result.hasIdDocument).toBe(false)
      expect(store.currentPatient?.hasIdDocument).toBe(false)
    })
  })

  describe('clearCurrentPatient', () => {
    it('should clear current patient', () => {
      const store = usePatientStore()
      store.currentPatient = mockPatient

      expect(store.currentPatient).not.toBeNull()

      store.clearCurrentPatient()

      expect(store.currentPatient).toBeNull()
    })
  })
})
