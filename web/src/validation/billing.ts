import { z } from 'zod'
import { ChargeType } from '@/types/billing'

export const createChargeSchema = z.object({
  chargeType: z.enum(
    [ChargeType.MEDICATION, ChargeType.PROCEDURE, ChargeType.LAB, ChargeType.SERVICE],
    { required_error: 'validation.billing.chargeTypeRequired' }
  ),
  description: z
    .string({ required_error: 'validation.billing.descriptionRequired' })
    .min(1, 'validation.billing.descriptionRequired')
    .max(500, 'validation.billing.descriptionMaxLength'),
  quantity: z
    .number({ required_error: 'validation.billing.quantityRequired' })
    .int('validation.billing.quantityMustBeInt')
    .positive('validation.billing.quantityPositive'),
  unitPrice: z
    .number({ required_error: 'validation.billing.unitPriceRequired' })
    .min(0, 'validation.billing.unitPriceNonNegative'),
  inventoryItemId: z.number().int().positive().optional().nullable()
})

export const createAdjustmentSchema = z.object({
  description: z
    .string({ required_error: 'validation.billing.descriptionRequired' })
    .min(1, 'validation.billing.descriptionRequired')
    .max(500, 'validation.billing.descriptionMaxLength'),
  amount: z
    .number({ required_error: 'validation.billing.amountRequired' })
    .negative('validation.billing.amountMustBeNegative'),
  reason: z
    .string({ required_error: 'validation.billing.reasonRequired' })
    .min(1, 'validation.billing.reasonRequired')
    .max(500, 'validation.billing.reasonMaxLength')
})

export type CreateChargeFormData = z.infer<typeof createChargeSchema>
export type CreateAdjustmentFormData = z.infer<typeof createAdjustmentSchema>
