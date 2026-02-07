import type { UserSummary } from './patient'

export enum RoomType {
  PRIVATE = 'PRIVATE',
  SHARED = 'SHARED'
}

export enum RoomGender {
  MALE = 'MALE',
  FEMALE = 'FEMALE'
}

export interface Room {
  id: number
  number: string
  type: RoomType
  gender: RoomGender
  capacity: number
  price: number | null
  cost: number | null
  createdAt: string | null
  createdBy: UserSummary | null
  updatedAt: string | null
  updatedBy: UserSummary | null
}

export interface RoomSummary {
  id: number
  number: string
  type: RoomType
  gender: RoomGender
}

export interface RoomAvailability {
  id: number
  number: string
  type: RoomType
  gender: RoomGender
  capacity: number
  availableBeds: number
}

export interface CreateRoomRequest {
  number: string
  type: RoomType
  gender: RoomGender
  capacity?: number
  price?: number | null
  cost?: number | null
}

export interface UpdateRoomRequest {
  number: string
  type: RoomType
  gender: RoomGender
  capacity?: number
  price?: number | null
  cost?: number | null
}
