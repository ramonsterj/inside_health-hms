import { z } from 'zod'

/**
 * Admission validation schemas that mirror backend Jakarta Bean Validation rules.
 */

/** Maximum file size for consent document uploads: 25MB */
export const MAX_CONSENT_FILE_SIZE = 25 * 1024 * 1024

/** Accepted MIME types for consent document uploads */
export const ACCEPTED_CONSENT_TYPES = 'image/*,application/pdf'

// Create admission schema
export const createAdmissionSchema = z.object({
  patientId: z
    .number({ required_error: 'Patient is required', invalid_type_error: 'Patient must be selected' })
    .positive('Patient is required'),
  triageCodeId: z
    .number({ required_error: 'Triage code is required', invalid_type_error: 'Triage code must be selected' })
    .positive('Triage code is required'),
  roomId: z
    .number({ required_error: 'Room is required', invalid_type_error: 'Room must be selected' })
    .positive('Room is required'),
  treatingPhysicianId: z
    .number({
      required_error: 'Treating physician is required',
      invalid_type_error: 'Treating physician must be selected'
    })
    .positive('Treating physician is required'),
  admissionDate: z.string().min(1, 'Admission date is required'),
  inventory: z
    .string()
    .max(2000, 'Inventory must be at most 2000 characters')
    .optional()
    .or(z.literal(''))
})

// Update admission schema (patientId and admissionDate are not editable)
export const updateAdmissionSchema = z.object({
  triageCodeId: z
    .number({ required_error: 'Triage code is required', invalid_type_error: 'Triage code must be selected' })
    .positive('Triage code is required'),
  roomId: z
    .number({ required_error: 'Room is required', invalid_type_error: 'Room must be selected' })
    .positive('Room is required'),
  treatingPhysicianId: z
    .number({
      required_error: 'Treating physician is required',
      invalid_type_error: 'Treating physician must be selected'
    })
    .positive('Treating physician is required'),
  inventory: z
    .string()
    .max(2000, 'Inventory must be at most 2000 characters')
    .optional()
    .or(z.literal(''))
})

// Consent file validation
export const consentFileSchema = z.object({
  file: z
    .instanceof(File, { message: 'Please select a file' })
    .refine((file) => file.size <= MAX_CONSENT_FILE_SIZE, 'File must be under 25MB')
    .refine(
      (file) => ['application/pdf', 'image/jpeg', 'image/png'].includes(file.type),
      'File must be PDF, JPEG, or PNG'
    )
})

// Add consulting physician schema
export const addConsultingPhysicianSchema = z.object({
  physicianId: z
    .number({
      required_error: 'Physician is required',
      invalid_type_error: 'Physician must be selected'
    })
    .positive('Physician is required'),
  reason: z
    .string()
    .max(500, 'Reason must be at most 500 characters')
    .optional()
    .or(z.literal('')),
  requestedDate: z.string().optional().or(z.literal(''))
})

// Type exports inferred from schemas
export type CreateAdmissionFormData = z.infer<typeof createAdmissionSchema>
export type UpdateAdmissionFormData = z.infer<typeof updateAdmissionSchema>
export type ConsentFileFormData = z.infer<typeof consentFileSchema>
export type AddConsultingPhysicianFormData = z.infer<typeof addConsultingPhysicianSchema>
