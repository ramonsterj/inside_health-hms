import { z } from 'zod'

/**
 * Triage code validation schemas that mirror backend Jakarta Bean Validation rules.
 * Error messages use i18n keys that are translated via zodI18n.ts
 */

// Triage code schema
export const triageCodeSchema = z.object({
  code: z
    .string()
    .min(1, 'validation.triageCode.code.required')
    .max(10, 'validation.triageCode.code.max'),
  color: z
    .string()
    .min(1, 'validation.triageCode.color.required')
    .regex(/^#?[0-9A-Fa-f]{6}$/, 'validation.triageCode.color.invalid'),
  description: z
    .string()
    .max(255, 'validation.triageCode.description.max')
    .optional()
    .or(z.literal('')),
  displayOrder: z
    .number({ invalid_type_error: 'validation.triageCode.displayOrder.invalid' })
    .int('validation.triageCode.displayOrder.integer')
    .min(0, 'validation.triageCode.displayOrder.min')
    .default(0)
})

// Type exports inferred from schemas
export type TriageCodeFormData = z.infer<typeof triageCodeSchema>
