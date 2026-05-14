import { z } from 'zod'

export const createLotSchema = z.object({
  lotNumber: z.string().max(50).optional().or(z.literal('')),
  expirationDate: z.string().min(1, 'validation.lot.expirationDate.required'),
  quantityOnHand: z.number({ required_error: 'validation.lot.quantity.required' }).int().min(0),
  receivedAt: z.string().min(1, 'validation.lot.receivedAt.required'),
  supplier: z.string().max(150).optional().or(z.literal('')),
  notes: z.string().max(500).optional().or(z.literal(''))
})

export const updateLotSchema = z.object({
  lotNumber: z.string().max(50).optional().or(z.literal('')),
  expirationDate: z.string().min(1, 'validation.lot.expirationDate.required'),
  supplier: z.string().max(150).optional().or(z.literal('')),
  notes: z.string().max(500).optional().or(z.literal('')),
  recalled: z.boolean().default(false),
  recalledReason: z.string().max(500).optional().or(z.literal(''))
})

export type LotFormData = z.infer<typeof createLotSchema>
export type UpdateLotFormData = z.infer<typeof updateLotSchema>
