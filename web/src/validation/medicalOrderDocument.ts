import { z } from 'zod'

/**
 * Medical order document validation schema.
 * Simplified version of document upload (no document type selector).
 */

const ALLOWED_CONTENT_TYPES = ['application/pdf', 'image/jpeg', 'image/png']
const MAX_FILE_SIZE = 25 * 1024 * 1024 // 25MB

export const uploadMedicalOrderDocumentSchema = z.object({
  file: z
    .instanceof(File, { message: 'validation.document.file.required' })
    .refine(file => file.size <= MAX_FILE_SIZE, 'validation.document.file.size')
    .refine(file => ALLOWED_CONTENT_TYPES.includes(file.type), 'validation.document.file.type'),
  displayName: z
    .string()
    .max(255, 'validation.document.displayName.max')
    .optional()
    .transform(val => (val === '' ? undefined : val))
})

export type UploadMedicalOrderDocumentFormData = z.infer<typeof uploadMedicalOrderDocumentSchema>
