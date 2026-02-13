import { z } from 'zod'

/**
 * Psychotherapy validation schemas that mirror backend Jakarta Bean Validation rules.
 * Error messages use i18n keys that are translated via zodI18n.ts
 */

// Psychotherapy category schema (admin form)
export const psychotherapyCategorySchema = z.object({
  name: z
    .string()
    .min(1, 'validation.psychotherapy.category.name.required')
    .max(100, 'validation.psychotherapy.category.name.max'),
  description: z
    .string()
    .max(255, 'validation.psychotherapy.category.description.max')
    .optional()
    .or(z.literal('')),
  displayOrder: z
    .number({ invalid_type_error: 'validation.psychotherapy.category.displayOrder.invalid' })
    .int('validation.psychotherapy.category.displayOrder.integer')
    .min(0, 'validation.psychotherapy.category.displayOrder.min')
    .default(0),
  active: z.boolean().default(true),
  price: z.number().min(0, 'validation.psychotherapy.category.price.min').optional().nullable(),
  cost: z.number().min(0, 'validation.psychotherapy.category.cost.min').optional().nullable()
})

// Psychotherapy activity schema (create form)
export const psychotherapyActivitySchema = z.object({
  categoryId: z
    .number({
      required_error: 'validation.psychotherapy.activity.category.required',
      invalid_type_error: 'validation.psychotherapy.activity.category.required'
    })
    .positive('validation.psychotherapy.activity.category.required'),
  description: z
    .string({
      required_error: 'validation.psychotherapy.activity.description.required'
    })
    .min(1, 'validation.psychotherapy.activity.description.required')
    .max(2000, 'validation.psychotherapy.activity.description.max')
})

// Type exports inferred from schemas
export type PsychotherapyCategoryFormData = z.infer<typeof psychotherapyCategorySchema>
export type PsychotherapyActivityFormData = z.infer<typeof psychotherapyActivitySchema>
