export enum UserStatus {
  ACTIVE = 'ACTIVE',
  INACTIVE = 'INACTIVE',
  SUSPENDED = 'SUSPENDED'
}

export enum Salutation {
  SR = 'SR',
  SRA = 'SRA',
  SRTA = 'SRTA',
  DR = 'DR',
  DRA = 'DRA',
  LIC = 'LIC',
  LICDA = 'LICDA',
  MR = 'MR',
  MRS = 'MRS',
  MISS = 'MISS'
}

export enum PhoneType {
  MOBILE = 'MOBILE',
  PRACTICE = 'PRACTICE',
  HOME = 'HOME',
  WORK = 'WORK',
  OTHER = 'OTHER'
}

export interface PhoneNumber {
  id: number
  phoneNumber: string
  phoneType: PhoneType
  isPrimary: boolean
}

export interface PhoneNumberRequest {
  id?: number
  phoneNumber: string
  phoneType: PhoneType
  isPrimary: boolean
}

export interface User {
  id: number
  username: string
  email: string
  firstName: string | null
  lastName: string | null
  salutation: Salutation | null
  salutationDisplay: string | null
  roles: string[]
  permissions: string[]
  status: UserStatus
  emailVerified: boolean
  mustChangePassword: boolean
  createdAt: string | null
  localePreference: string | null
  phoneNumbers: PhoneNumber[]
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
  salutation?: Salutation | null
  roleCodes?: string[]
  status?: UserStatus
  emailVerified?: boolean
  phoneNumbers: PhoneNumberRequest[] // Required - at least one phone number
}

export interface ResetPasswordResponse {
  temporaryPassword: string
}

export interface AdminUpdateUserRequest {
  firstName?: string | null
  lastName?: string | null
  salutation?: Salutation | null
  roleCodes?: string[]
  status?: UserStatus
  emailVerified?: boolean
  phoneNumbers?: PhoneNumberRequest[]
}
