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

export interface BedOccupant {
  admissionId: number
  patientId: number
  patientName: string
  admissionDate: string
}

export interface RoomOccupancyItem {
  id: number
  number: string
  type: RoomType
  gender: RoomGender
  capacity: number
  occupiedBeds: number
  availableBeds: number
  occupants: BedOccupant[]
}

export interface OccupancySummary {
  totalBeds: number
  occupiedBeds: number
  freeBeds: number
  occupancyPercent: number
}

export interface BedOccupancyResponse {
  summary: OccupancySummary
  rooms: RoomOccupancyItem[]
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
