import { z } from 'zod'

/**
 * Triage code validation schemas that mirror backend Jakarta Bean Validation rules.
 */

// Triage code schema
export const triageCodeSchema = z.object({
  code: z
    .string()
    .min(1, 'Code is required')
    .max(10, 'Code must be at most 10 characters'),
  color: z
    .string()
    .min(1, 'Color is required')
    .regex(/^#?[0-9A-Fa-f]{6}$/, 'Must be a valid hex color (e.g., #FF0000)'),
  description: z
    .string()
    .max(255, 'Description must be at most 255 characters')
    .optional()
    .or(z.literal('')),
  displayOrder: z
    .number({ invalid_type_error: 'Display order must be a number' })
    .int('Display order must be an integer')
    .min(0, 'Display order must be at least 0')
    .default(0)
})

// Type exports inferred from schemas
export type TriageCodeFormData = z.infer<typeof triageCodeSchema>
