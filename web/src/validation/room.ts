import { z } from 'zod'
import { RoomType } from '@/types/room'

/**
 * Room validation schemas that mirror backend Jakarta Bean Validation rules.
 */

// Room schema
export const roomSchema = z.object({
  number: z
    .string()
    .min(1, 'Room number is required')
    .max(50, 'Room number must be at most 50 characters'),
  type: z.nativeEnum(RoomType, { required_error: 'Room type is required' }),
  capacity: z
    .number({ required_error: 'Capacity is required', invalid_type_error: 'Capacity must be a number' })
    .int('Capacity must be an integer')
    .min(1, 'Capacity must be at least 1')
    .default(1)
})

// Type exports inferred from schemas
export type RoomFormData = z.infer<typeof roomSchema>
