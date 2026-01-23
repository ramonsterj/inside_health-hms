import { z } from 'zod'

/**
 * User management validation schemas.
 * Used for admin user creation and editing.
 */

// Enum schemas
export const salutationEnum = z.enum(['SR', 'SRA', 'SRTA', 'DR', 'DRA', 'MR', 'MRS', 'MISS'])
export const phoneTypeEnum = z.enum(['MOBILE', 'PRACTICE', 'HOME', 'WORK', 'OTHER'])
export const userStatusEnum = z.enum(['ACTIVE', 'INACTIVE', 'SUSPENDED'])

// Phone number schema
export const phoneNumberSchema = z.object({
  id: z.number().optional(),
  phoneNumber: z
    .string()
    .min(1, 'validation.phone.number.required')
    .max(20, 'validation.phone.number.max'),
  phoneType: phoneTypeEnum,
  isPrimary: z.boolean().default(false)
})

// Create user schema (for admin form with confirm password)
export const createUserSchema = z
  .object({
    username: z.string().min(3, 'validation.username.min').max(50, 'validation.username.max'),
    email: z.string().email('validation.email.invalid').max(255, 'validation.email.max'),
    password: z.string().min(8, 'validation.password.min'),
    confirmPassword: z.string().min(1, 'validation.password.confirm.required'),
    firstName: z.string().max(100, 'validation.firstName.max').optional().or(z.literal('')),
    lastName: z.string().max(100, 'validation.lastName.max').optional().or(z.literal('')),
    salutation: salutationEnum.optional().nullable(),
    roleCodes: z.array(z.string()).min(1, 'validation.roles.required'),
    status: userStatusEnum.default('ACTIVE'),
    emailVerified: z.boolean().default(false),
    phoneNumbers: z.array(phoneNumberSchema).min(1, 'validation.phone.required')
  })
  .refine(data => data.password === data.confirmPassword, {
    message: 'validation.password.mismatch',
    path: ['confirmPassword']
  })

// Admin update user schema
export const adminUpdateUserSchema = z.object({
  firstName: z.string().max(100, 'validation.firstName.max').optional().or(z.literal('')),
  lastName: z.string().max(100, 'validation.lastName.max').optional().or(z.literal('')),
  salutation: salutationEnum.optional().nullable(),
  roleCodes: z.array(z.string()).min(1, 'validation.roles.required'),
  status: userStatusEnum,
  emailVerified: z.boolean(),
  phoneNumbers: z.array(phoneNumberSchema).min(1, 'validation.phone.required')
})

// Force change password schema (for first login)
export const forceChangePasswordSchema = z
  .object({
    currentPassword: z.string().min(1, 'validation.password.current.required'),
    newPassword: z.string().min(8, 'validation.password.min'),
    confirmNewPassword: z.string().min(1, 'validation.password.confirm.required')
  })
  .refine(data => data.newPassword === data.confirmNewPassword, {
    message: 'validation.password.mismatch',
    path: ['confirmNewPassword']
  })
  .refine(data => data.currentPassword !== data.newPassword, {
    message: 'validation.password.mustBeDifferent',
    path: ['newPassword']
  })

// Type exports
export type PhoneNumberFormData = z.infer<typeof phoneNumberSchema>
export type CreateUserFormData = z.infer<typeof createUserSchema>
export type AdminUpdateUserFormData = z.infer<typeof adminUpdateUserSchema>
export type ForceChangePasswordFormData = z.infer<typeof forceChangePasswordSchema>
