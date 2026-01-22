import { z } from 'zod'

/**
 * Patient validation schemas that mirror backend Jakarta Bean Validation rules.
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
    .min(1, 'Contact name is required')
    .max(200, 'Contact name must be at most 200 characters'),
  relationship: z
    .string()
    .min(1, 'Relationship is required')
    .max(100, 'Relationship must be at most 100 characters'),
  phone: z.string().min(1, 'Phone is required').max(20, 'Phone must be at most 20 characters')
})

// Patient schema
export const patientSchema = z.object({
  firstName: z
    .string()
    .min(1, 'First name is required')
    .max(100, 'First name must be at most 100 characters'),
  lastName: z
    .string()
    .min(1, 'Last name is required')
    .max(100, 'Last name must be at most 100 characters'),
  age: z
    .number({ required_error: 'Age is required', invalid_type_error: 'Age must be a number' })
    .min(0, 'Age must be at least 0')
    .max(150, 'Age must be at most 150'),
  sex: z.enum(['MALE', 'FEMALE'], { required_error: 'Sex is required' }),
  gender: z.string().min(1, 'Gender is required').max(50, 'Gender must be at most 50 characters'),
  maritalStatus: z.enum(['SINGLE', 'MARRIED', 'DIVORCED', 'WIDOWED', 'SEPARATED', 'OTHER'], {
    required_error: 'Marital status is required'
  }),
  religion: z
    .string()
    .min(1, 'Religion is required')
    .max(100, 'Religion must be at most 100 characters'),
  educationLevel: z.enum(
    ['NONE', 'PRIMARY', 'SECONDARY', 'TECHNICAL', 'UNIVERSITY', 'POSTGRADUATE'],
    { required_error: 'Education level is required' }
  ),
  occupation: z
    .string()
    .min(1, 'Occupation is required')
    .max(100, 'Occupation must be at most 100 characters'),
  address: z
    .string()
    .min(1, 'Address is required')
    .max(500, 'Address must be at most 500 characters'),
  email: z
    .string()
    .min(1, 'Email is required')
    .email('Invalid email format')
    .max(255, 'Email must be at most 255 characters'),
  idDocumentNumber: z
    .string()
    .max(50, 'ID document number must be at most 50 characters')
    .optional()
    .or(z.literal('')),
  notes: z.string().optional().or(z.literal('')),
  emergencyContacts: z
    .array(emergencyContactSchema)
    .min(1, 'At least one emergency contact is required')
})

// Type exports inferred from schemas
export type EmergencyContactFormData = z.infer<typeof emergencyContactSchema>
export type PatientFormData = z.infer<typeof patientSchema>
