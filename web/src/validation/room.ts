import { z } from 'zod'
import { RoomType, RoomGender } from '@/types/room'

/**
 * Room validation schemas that mirror backend Jakarta Bean Validation rules.
 * Error messages use i18n keys that are translated via zodI18n.ts
 */

// Room schema
export const roomSchema = z.object({
  number: z
    .string()
    .min(1, 'validation.room.number.required')
    .max(50, 'validation.room.number.max'),
  type: z.nativeEnum(RoomType, { required_error: 'validation.room.type.required' }),
  gender: z.nativeEnum(RoomGender, { required_error: 'validation.room.gender.required' }),
  capacity: z
    .number({
      required_error: 'validation.room.capacity.required',
      invalid_type_error: 'validation.room.capacity.invalid'
    })
    .int('validation.room.capacity.integer')
    .min(1, 'validation.room.capacity.min')
    .default(1),
  price: z
    .number()
    .min(0, 'validation.inventory.room.price.min')
    .nullable()
    .optional(),
  cost: z
    .number()
    .min(0, 'validation.inventory.room.cost.min')
    .nullable()
    .optional()
})

// Type exports inferred from schemas
export type RoomFormData = z.infer<typeof roomSchema>
