import { z } from 'zod'

/**
 * Document validation schemas that mirror backend validation rules.
 * Error messages use i18n keys that are translated via zodI18n.ts
 */

// Allowed file types and max size - must match backend: AdmissionDocument.kt
const ALLOWED_CONTENT_TYPES = ['application/pdf', 'image/jpeg', 'image/png']
const MAX_FILE_SIZE = 25 * 1024 * 1024 // 25MB

// Upload document schema
export const uploadDocumentSchema = z.object({
  file: z
    .instanceof(File, { message: 'validation.document.file.required' })
    .refine(file => file.size <= MAX_FILE_SIZE, 'validation.document.file.size')
    .refine(file => ALLOWED_CONTENT_TYPES.includes(file.type), 'validation.document.file.type'),
  documentTypeId: z
    .number({
      required_error: 'validation.document.type.required',
      invalid_type_error: 'validation.document.type.invalid'
    })
    .positive('validation.document.type.required'),
  displayName: z
    .string()
    .max(255, 'validation.document.displayName.max')
    .optional()
    .transform(val => (val === '' ? undefined : val))
})

// Document type schema (for admin CRUD)
export const documentTypeSchema = z.object({
  code: z
    .string()
    .min(1, 'validation.documentType.code.required')
    .max(50, 'validation.documentType.code.max')
    .regex(/^[A-Z_]+$/, 'validation.documentType.code.pattern'),
  name: z
    .string()
    .min(1, 'validation.documentType.name.required')
    .max(100, 'validation.documentType.name.max'),
  description: z
    .string()
    .max(255, 'validation.documentType.description.max')
    .optional()
    .nullable()
    .transform(val => (val === '' ? null : val)),
  displayOrder: z
    .number({
      required_error: 'validation.documentType.displayOrder.required',
      invalid_type_error: 'validation.documentType.displayOrder.invalid'
    })
    .int('validation.documentType.displayOrder.integer')
    .min(0, 'validation.documentType.displayOrder.min')
    .default(0)
})

// Type exports inferred from schemas
export type UploadDocumentFormData = z.infer<typeof uploadDocumentSchema>
export type DocumentTypeFormData = z.infer<typeof documentTypeSchema>
