import { z } from 'zod'
import {
  AdmissionType,
  admissionTypeRequiresRoom,
  admissionTypeRequiresTriageCode
} from '@/types/admission'

/**
 * Admission validation schemas that mirror backend Jakarta Bean Validation rules.
 * Error messages use i18n keys that are translated via zodI18n.ts
 */

/** Maximum file size for consent document uploads: 25MB */
export const MAX_CONSENT_FILE_SIZE = 25 * 1024 * 1024

/** Accepted MIME types for consent document uploads */
export const ACCEPTED_CONSENT_TYPES = 'image/*,application/pdf'

/** Valid admission type values */
const admissionTypeValues = [
  AdmissionType.HOSPITALIZATION,
  AdmissionType.AMBULATORY,
  AdmissionType.ELECTROSHOCK_THERAPY,
  AdmissionType.KETAMINE_INFUSION,
  AdmissionType.EMERGENCY
] as const

// Create admission schema
export const createAdmissionSchema = z
  .object({
    patientId: z
      .number({
        required_error: 'validation.admission.patientId.required',
        invalid_type_error: 'validation.admission.patientId.required'
      })
      .positive('validation.admission.patientId.required'),
    triageCodeId: z
      .number({
        invalid_type_error: 'validation.admission.triageCodeId.required'
      })
      .positive('validation.admission.triageCodeId.required')
      .nullable(),
    roomId: z
      .number({
        invalid_type_error: 'validation.admission.roomId.required'
      })
      .positive('validation.admission.roomId.required')
      .nullable(),
    treatingPhysicianId: z
      .number({
        required_error: 'validation.admission.treatingPhysicianId.required',
        invalid_type_error: 'validation.admission.treatingPhysicianId.required'
      })
      .positive('validation.admission.treatingPhysicianId.required'),
    admissionDate: z.string().min(1, 'validation.admission.admissionDate.required'),
    type: z.enum(admissionTypeValues, {
      required_error: 'validation.admission.type.required',
      invalid_type_error: 'validation.admission.type.required'
    }),
    inventory: z
      .string()
      .max(2000, 'validation.admission.inventory.max')
      .optional()
      .or(z.literal(''))
  })
  .refine(
    data => {
      // Triage code is required for HOSPITALIZATION and EMERGENCY types
      if (admissionTypeRequiresTriageCode(data.type)) {
        return data.triageCodeId !== null && data.triageCodeId > 0
      }
      return true
    },
    {
      message: 'validation.admission.triageCodeId.requiredForType',
      path: ['triageCodeId']
    }
  )
  .refine(
    data => {
      // Room is required only for HOSPITALIZATION type
      if (admissionTypeRequiresRoom(data.type)) {
        return data.roomId !== null && data.roomId > 0
      }
      return true
    },
    {
      message: 'validation.admission.roomId.requiredForType',
      path: ['roomId']
    }
  )

// Update admission schema (patientId and admissionDate are not editable)
export const updateAdmissionSchema = z
  .object({
    triageCodeId: z
      .number({
        invalid_type_error: 'validation.admission.triageCodeId.required'
      })
      .positive('validation.admission.triageCodeId.required')
      .nullable(),
    roomId: z
      .number({
        invalid_type_error: 'validation.admission.roomId.required'
      })
      .positive('validation.admission.roomId.required')
      .nullable(),
    treatingPhysicianId: z
      .number({
        required_error: 'validation.admission.treatingPhysicianId.required',
        invalid_type_error: 'validation.admission.treatingPhysicianId.required'
      })
      .positive('validation.admission.treatingPhysicianId.required'),
    type: z.enum(admissionTypeValues).optional(),
    inventory: z
      .string()
      .max(2000, 'validation.admission.inventory.max')
      .optional()
      .or(z.literal(''))
  })
  .refine(
    data => {
      // Triage code is required for HOSPITALIZATION and EMERGENCY types (if type is specified)
      if (data.type && admissionTypeRequiresTriageCode(data.type)) {
        return data.triageCodeId !== null && data.triageCodeId > 0
      }
      return true
    },
    {
      message: 'validation.admission.triageCodeId.requiredForType',
      path: ['triageCodeId']
    }
  )
  .refine(
    data => {
      // Room is required only for HOSPITALIZATION type (if type is specified)
      if (data.type && admissionTypeRequiresRoom(data.type)) {
        return data.roomId !== null && data.roomId > 0
      }
      return true
    },
    {
      message: 'validation.admission.roomId.requiredForType',
      path: ['roomId']
    }
  )

// Consent file validation
export const consentFileSchema = z.object({
  file: z
    .instanceof(File, { message: 'validation.admission.consent.required' })
    .refine(file => file.size <= MAX_CONSENT_FILE_SIZE, 'validation.admission.consent.size')
    .refine(
      file => ['application/pdf', 'image/jpeg', 'image/png'].includes(file.type),
      'validation.admission.consent.type'
    )
})

// Add consulting physician schema
export const addConsultingPhysicianSchema = z.object({
  physicianId: z
    .number({
      required_error: 'validation.admission.consultingPhysicians.physicianId.required',
      invalid_type_error: 'validation.admission.consultingPhysicians.physicianId.required'
    })
    .positive('validation.admission.consultingPhysicians.physicianId.required'),
  reason: z
    .string()
    .max(500, 'validation.admission.consultingPhysicians.reason.max')
    .optional()
    .or(z.literal('')),
  requestedDate: z.string().optional().or(z.literal(''))
})

// Type exports inferred from schemas
export type CreateAdmissionFormData = z.infer<typeof createAdmissionSchema>
export type UpdateAdmissionFormData = z.infer<typeof updateAdmissionSchema>
export type ConsentFileFormData = z.infer<typeof consentFileSchema>
export type AddConsultingPhysicianFormData = z.infer<typeof addConsultingPhysicianSchema>
