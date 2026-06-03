import { z } from 'zod'

/**
 * Lab catalog validation schemas that mirror the backend Jakarta Bean Validation rules.
 * Error messages use i18n keys translated via zodI18n.ts.
 */

export const labProviderSchema = z.object({
  name: z
    .string()
    .min(1, 'validation.lab.provider.name.required')
    .max(150, 'validation.lab.provider.name.max'),
  code: z.string().max(50, 'validation.lab.provider.code.max').optional().or(z.literal('')),
  active: z.boolean().default(true)
})

export const labTestSchema = z.object({
  name: z
    .string()
    .min(1, 'validation.lab.test.name.required')
    .max(200, 'validation.lab.test.name.max'),
  active: z.boolean().default(true)
})

export const createLabProviderTestSchema = z.object({
  labTestId: z
    .number({
      required_error: 'validation.lab.providerTest.labTest.required',
      invalid_type_error: 'validation.lab.providerTest.labTest.required'
    })
    .int()
    .positive('validation.lab.providerTest.labTest.required'),
  displayName: z
    .string()
    .max(200, 'validation.lab.providerTest.displayName.max')
    .optional()
    .or(z.literal('')),
  cost: z
    .number({ invalid_type_error: 'validation.lab.providerTest.cost.invalid' })
    .min(0, 'validation.lab.providerTest.cost.min'),
  salesPrice: z
    .number({ invalid_type_error: 'validation.lab.providerTest.salesPrice.invalid' })
    .positive('validation.lab.providerTest.salesPrice.positive'),
  active: z.boolean().default(true)
})

// Update PUT contract omits labTestId.
export const updateLabProviderTestSchema = createLabProviderTestSchema.omit({ labTestId: true })

export const labPanelSchema = z.object({
  name: z
    .string()
    .min(1, 'validation.lab.panel.name.required')
    .max(200, 'validation.lab.panel.name.max'),
  active: z.boolean().default(true),
  labTestIds: z.array(z.number().int().positive()).min(1, 'validation.lab.panel.tests.required')
})

export type LabProviderFormData = z.infer<typeof labProviderSchema>
export type LabTestFormData = z.infer<typeof labTestSchema>
export type LabProviderTestFormData = z.infer<typeof createLabProviderTestSchema>
export type LabPanelFormData = z.infer<typeof labPanelSchema>
