import { z } from 'zod'
import { PricingType, TimeUnit, MovementType } from '@/types/inventoryItem'

export const inventoryCategorySchema = z.object({
  name: z
    .string()
    .min(1, 'validation.inventory.category.name.required')
    .max(100, 'validation.inventory.category.name.max'),
  description: z
    .string()
    .max(255, 'validation.inventory.category.description.max')
    .optional()
    .or(z.literal('')),
  displayOrder: z
    .number({ invalid_type_error: 'validation.inventory.category.displayOrder.invalid' })
    .int()
    .min(0)
    .default(0),
  active: z.boolean().default(true)
})

export const inventoryItemSchema = z
  .object({
    name: z
      .string()
      .min(1, 'validation.inventory.item.name.required')
      .max(150, 'validation.inventory.item.name.max'),
    description: z
      .string()
      .max(500, 'validation.inventory.item.description.max')
      .optional()
      .or(z.literal('')),
    categoryId: z
      .number({ required_error: 'validation.inventory.item.category.required' })
      .positive(),
    price: z
      .number({ required_error: 'validation.inventory.item.price.required' })
      .min(0, 'validation.inventory.item.price.min'),
    cost: z
      .number({ required_error: 'validation.inventory.item.cost.required' })
      .min(0, 'validation.inventory.item.cost.min'),
    restockLevel: z.number().int().min(0).default(0),
    pricingType: z.nativeEnum(PricingType).default(PricingType.FLAT),
    timeUnit: z.nativeEnum(TimeUnit).nullable().optional(),
    timeInterval: z.number().int().positive().nullable().optional(),
    active: z.boolean().default(true)
  })
  .refine(
    (data) =>
      data.pricingType !== PricingType.TIME_BASED || (data.timeUnit != null && data.timeInterval != null),
    { message: 'validation.inventory.item.timeBased.required', path: ['timeUnit'] }
  )

export const inventoryMovementSchema = z.object({
  type: z.nativeEnum(MovementType, {
    required_error: 'validation.inventory.movement.type.required'
  }),
  quantity: z
    .number({ required_error: 'validation.inventory.movement.quantity.required' })
    .int()
    .positive('validation.inventory.movement.quantity.positive'),
  notes: z
    .string()
    .max(500, 'validation.inventory.movement.notes.max')
    .optional()
    .or(z.literal(''))
})

export type InventoryCategoryFormData = z.infer<typeof inventoryCategorySchema>
export type InventoryItemFormData = z.infer<typeof inventoryItemSchema>
export type InventoryMovementFormData = z.infer<typeof inventoryMovementSchema>
