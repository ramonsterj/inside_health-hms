import { z } from 'zod'

/**
 * Validation schemas that mirror backend Jakarta Bean Validation rules.
 * Keep these in sync with backend DTOs.
 * Error messages use i18n keys that are translated via zodI18n.ts
 */

// Common field validators
export const username = z
  .string()
  .min(3, 'validation.username.min')
  .max(50, 'validation.username.max')

export const email = z
  .string()
  .email('validation.emailField.invalid')
  .max(255, 'validation.name.max')

export const password = z.string().min(8, 'validation.password.min')

export const optionalName = z.string().max(100, 'validation.name.max').optional().or(z.literal(''))

// Auth schemas
export const loginSchema = z.object({
  identifier: z.string().min(1, 'validation.username.required'),
  password: z.string().min(1, 'validation.password.required')
})

export const registerSchema = z
  .object({
    username,
    email,
    password,
    confirmPassword: z.string().min(1, 'validation.password.confirm'),
    firstName: optionalName,
    lastName: optionalName
  })
  .refine(data => data.password === data.confirmPassword, {
    message: 'validation.passwordMatch',
    path: ['confirmPassword']
  })

// Profile schemas
export const profileUpdateSchema = z.object({
  firstName: optionalName,
  lastName: optionalName
})

export const changePasswordSchema = z
  .object({
    currentPassword: z.string().min(1, 'validation.password.current'),
    newPassword: password,
    confirmPassword: z.string().min(1, 'validation.password.confirm')
  })
  .refine(data => data.newPassword === data.confirmPassword, {
    message: 'validation.passwordMatch',
    path: ['confirmPassword']
  })

// Type exports inferred from schemas
export type LoginFormData = z.infer<typeof loginSchema>
export type RegisterFormData = z.infer<typeof registerSchema>
export type ProfileUpdateFormData = z.infer<typeof profileUpdateSchema>
export type ChangePasswordFormData = z.infer<typeof changePasswordSchema>
