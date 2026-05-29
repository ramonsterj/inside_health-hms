import { z } from 'zod'

export const warehouseSchema = z.object({
  code: z
    .string()
    .min(1, 'validation.warehouse.code.required')
    .max(50, 'validation.warehouse.code.max')
    .regex(/^[A-Z0-9_]+$/, 'validation.warehouse.code.format'),
  name: z
    .string()
    .min(1, 'validation.warehouse.name.required')
    .max(150, 'validation.warehouse.name.max'),
  description: z
    .string()
    .max(500, 'validation.warehouse.description.max')
    .optional()
    .or(z.literal('')),
  active: z.boolean().default(true)
})

export const createTransferSchema = z
  .object({
    sourceWarehouseId: z
      .number({ required_error: 'validation.transfer.source.required' })
      .positive('validation.transfer.source.required'),
    destinationWarehouseId: z
      .number({ required_error: 'validation.transfer.destination.required' })
      .positive('validation.transfer.destination.required'),
    itemId: z
      .number({ required_error: 'validation.transfer.item.required' })
      .positive('validation.transfer.item.required'),
    lotId: z.number().positive().nullable().optional(),
    quantity: z
      .number({ required_error: 'validation.transfer.quantity.required' })
      .int()
      .positive('validation.transfer.quantity.positive'),
    notes: z.string().max(500, 'validation.transfer.notes.max').optional().or(z.literal(''))
  })
  .refine(data => data.sourceWarehouseId !== data.destinationWarehouseId, {
    message: 'validation.transfer.source-equals-destination',
    path: ['destinationWarehouseId']
  })

export const createWarehouseChargeSchema = z.object({
  warehouseId: z
    .number({ required_error: 'validation.warehouseCharge.warehouse.required' })
    .positive('validation.warehouseCharge.warehouse.required'),
  itemId: z
    .number({ required_error: 'validation.warehouseCharge.item.required' })
    .positive('validation.warehouseCharge.item.required'),
  lotId: z.number().positive().nullable().optional(),
  admissionId: z
    .number({ required_error: 'validation.warehouseCharge.admission.required' })
    .positive('validation.warehouseCharge.admission.required'),
  quantity: z
    .number({ required_error: 'validation.warehouseCharge.quantity.required' })
    .int()
    .positive('validation.warehouseCharge.quantity.positive'),
  reason: z
    .string()
    .min(1, 'validation.warehouseCharge.reason.required')
    .max(500, 'validation.warehouseCharge.reason.max'),
  notes: z.string().max(500, 'validation.warehouseCharge.notes.max').optional().or(z.literal(''))
})

export type WarehouseFormData = z.infer<typeof warehouseSchema>
export type CreateTransferFormData = z.infer<typeof createTransferSchema>
export type CreateWarehouseChargeFormData = z.infer<typeof createWarehouseChargeSchema>
