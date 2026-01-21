import { z } from 'zod'

/**
 * Validation schemas that mirror backend Jakarta Bean Validation rules.
 * Keep these in sync with backend DTOs.
 */

// Common field validators
export const username = z
  .string()
  .min(3, 'Username must be at least 3 characters')
  .max(50, 'Username must be at most 50 characters')

export const email = z.string().email('Invalid email format').max(255, 'Email is too long')

export const password = z.string().min(8, 'Password must be at least 8 characters')

export const optionalName = z
  .string()
  .max(100, 'Name must be at most 100 characters')
  .optional()
  .or(z.literal(''))

// Auth schemas
export const loginSchema = z.object({
  identifier: z.string().min(1, 'Email or username is required'),
  password: z.string().min(1, 'Password is required')
})

export const registerSchema = z
  .object({
    username,
    email,
    password,
    confirmPassword: z.string().min(1, 'Please confirm your password'),
    firstName: optionalName,
    lastName: optionalName
  })
  .refine(data => data.password === data.confirmPassword, {
    message: 'Passwords do not match',
    path: ['confirmPassword']
  })

// Profile schemas
export const profileUpdateSchema = z.object({
  firstName: optionalName,
  lastName: optionalName
})

export const changePasswordSchema = z
  .object({
    currentPassword: z.string().min(1, 'Current password is required'),
    newPassword: password,
    confirmPassword: z.string().min(1, 'Please confirm your password')
  })
  .refine(data => data.newPassword === data.confirmPassword, {
    message: 'Passwords do not match',
    path: ['confirmPassword']
  })

// Type exports inferred from schemas
export type LoginFormData = z.infer<typeof loginSchema>
export type RegisterFormData = z.infer<typeof registerSchema>
export type ProfileUpdateFormData = z.infer<typeof profileUpdateSchema>
export type ChangePasswordFormData = z.infer<typeof changePasswordSchema>
