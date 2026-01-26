import type { UserSummary } from './patient'

export enum RoomType {
  PRIVATE = 'PRIVATE',
  SHARED = 'SHARED'
}

export interface Room {
  id: number
  number: string
  type: RoomType
  capacity: number
  createdAt: string | null
  createdBy: UserSummary | null
  updatedAt: string | null
  updatedBy: UserSummary | null
}

export interface RoomSummary {
  id: number
  number: string
  type: RoomType
}

export interface RoomAvailability {
  id: number
  number: string
  type: RoomType
  capacity: number
  availableBeds: number
}

export interface CreateRoomRequest {
  number: string
  type: RoomType
  capacity?: number
}

export interface UpdateRoomRequest {
  number: string
  type: RoomType
  capacity?: number
}
