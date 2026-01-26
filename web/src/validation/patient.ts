import { z } from 'zod'

/**
 * Patient validation schemas that mirror backend Jakarta Bean Validation rules.
 * Error messages use i18n keys that are translated via zodI18n.ts
 */

/** Maximum file size for ID document uploads: 5MB */
export const MAX_ID_DOCUMENT_SIZE = 5 * 1024 * 1024

/** Accepted MIME types for ID document uploads */
export const ACCEPTED_ID_DOCUMENT_TYPES = 'image/*,application/pdf'

// Emergency contact schema
export const emergencyContactSchema = z.object({
  id: z.number().optional(),
  name: z
    .string()
    .min(1, 'validation.patient.emergencyContacts.name.required')
    .max(200, 'validation.patient.emergencyContacts.name.max'),
  relationship: z
    .string()
    .min(1, 'validation.patient.emergencyContacts.relationship.required')
    .max(100, 'validation.patient.emergencyContacts.relationship.max'),
  phone: z
    .string()
    .min(1, 'validation.patient.emergencyContacts.phone.required')
    .max(20, 'validation.patient.emergencyContacts.phone.max')
})

// Patient schema
export const patientSchema = z.object({
  firstName: z
    .string()
    .min(1, 'validation.patient.firstName.required')
    .max(100, 'validation.patient.firstName.max'),
  lastName: z
    .string()
    .min(1, 'validation.patient.lastName.required')
    .max(100, 'validation.patient.lastName.max'),
  age: z
    .number({
      required_error: 'validation.patient.age.required',
      invalid_type_error: 'validation.patient.age.invalid'
    })
    .min(0, 'validation.patient.age.min')
    .max(150, 'validation.patient.age.max'),
  sex: z.enum(['MALE', 'FEMALE'], { required_error: 'validation.patient.sex.required' }),
  gender: z
    .string()
    .min(1, 'validation.patient.gender.required')
    .max(50, 'validation.patient.gender.max'),
  maritalStatus: z.enum(['SINGLE', 'MARRIED', 'DIVORCED', 'WIDOWED', 'SEPARATED', 'OTHER'], {
    required_error: 'validation.patient.maritalStatus.required'
  }),
  religion: z
    .string()
    .min(1, 'validation.patient.religion.required')
    .max(100, 'validation.patient.religion.max'),
  educationLevel: z.enum(
    ['NONE', 'PRIMARY', 'SECONDARY', 'TECHNICAL', 'UNIVERSITY', 'POSTGRADUATE'],
    { required_error: 'validation.patient.educationLevel.required' }
  ),
  occupation: z
    .string()
    .min(1, 'validation.patient.occupation.required')
    .max(100, 'validation.patient.occupation.max'),
  address: z
    .string()
    .min(1, 'validation.patient.address.required')
    .max(500, 'validation.patient.address.max'),
  email: z
    .string()
    .min(1, 'validation.patient.email.required')
    .email('validation.patient.email.invalid')
    .max(255, 'validation.patient.email.max'),
  idDocumentNumber: z
    .string()
    .max(50, 'validation.patient.idDocumentNumber.max')
    .optional()
    .or(z.literal('')),
  notes: z.string().optional().or(z.literal('')),
  emergencyContacts: z
    .array(emergencyContactSchema)
    .min(1, 'validation.patient.emergencyContacts.required')
})

// Type exports inferred from schemas
export type EmergencyContactFormData = z.infer<typeof emergencyContactSchema>
export type PatientFormData = z.infer<typeof patientSchema>
