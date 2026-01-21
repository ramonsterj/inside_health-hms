import type { User } from './user'

export interface LoginRequest {
  identifier: string
  password: string
}

export interface RegisterRequest {
  username: string
  email: string
  password: string
  firstName?: string | null
  lastName?: string | null
}

export interface RefreshTokenRequest {
  refreshToken: string
}

export interface AuthResponse {
  accessToken: string
  refreshToken: string
  tokenType: string
  expiresIn: number
  user: User
}

export interface UsernameAvailabilityResponse {
  available: boolean
  message: string | null
}
