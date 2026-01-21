export enum UserRole {
  ADMIN = 'ADMIN',
  USER = 'USER'
}

export enum UserStatus {
  ACTIVE = 'ACTIVE',
  INACTIVE = 'INACTIVE',
  SUSPENDED = 'SUSPENDED'
}

export interface User {
  id: number
  username: string
  email: string
  firstName: string | null
  lastName: string | null
  roles: string[]
  permissions: string[]
  status: UserStatus
  emailVerified: boolean
  createdAt: string | null
  localePreference: string | null
}

export interface UpdateUserRequest {
  firstName?: string | null
  lastName?: string | null
}

export interface ChangePasswordRequest {
  currentPassword: string
  newPassword: string
}

export interface CreateUserRequest {
  username: string
  email: string
  password: string
  firstName?: string | null
  lastName?: string | null
  roleCodes?: string[]
  status?: UserStatus
  emailVerified?: boolean
}

export interface ResetPasswordResponse {
  temporaryPassword: string
}

export interface AdminUpdateUserRequest {
  firstName?: string | null
  lastName?: string | null
  roleCodes?: string[]
  status?: UserStatus
  emailVerified?: boolean
}
