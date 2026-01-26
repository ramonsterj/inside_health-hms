import { describe, it, expect, beforeEach, vi, afterEach, type Mock } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useRoomStore } from './room'
import api from '@/services/api'
import type { Room, RoomAvailability } from '@/types/room'
import { RoomType } from '@/types/room'

vi.mock('@/services/api', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn()
  }
}))

const mockedApi = api as unknown as {
  get: Mock
  post: Mock
  put: Mock
  delete: Mock
}

const mockRoom: Room = {
  id: 1,
  number: '101',
  type: RoomType.PRIVATE,
  capacity: 1,
  createdAt: null,
  createdBy: null,
  updatedAt: null,
  updatedBy: null
}

const mockRoomAvailability: RoomAvailability = {
  id: 1,
  number: '101',
  type: RoomType.PRIVATE,
  capacity: 1,
  availableBeds: 1
}

describe('useRoomStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  afterEach(() => {
    vi.resetAllMocks()
  })

  describe('initial state', () => {
    it('should start with empty state', () => {
      const store = useRoomStore()

      expect(store.rooms).toEqual([])
      expect(store.availableRooms).toEqual([])
      expect(store.currentRoom).toBeNull()
      expect(store.loading).toBe(false)
    })
  })

  describe('fetchRooms', () => {
    it('should fetch rooms and update state', async () => {
      mockedApi.get.mockResolvedValueOnce({
        data: {
          success: true,
          data: [mockRoom]
        }
      })

      const store = useRoomStore()
      await store.fetchRooms()

      expect(mockedApi.get).toHaveBeenCalledWith('/v1/rooms')
      expect(store.rooms).toHaveLength(1)
      expect(store.rooms[0]).toEqual(mockRoom)
    })

    it('should handle loading state', async () => {
      let resolvePromise: (value: unknown) => void = () => {}
      const promise = new Promise(resolve => {
        resolvePromise = resolve
      })
      mockedApi.get.mockReturnValueOnce(promise as Promise<{ data: unknown }>)

      const store = useRoomStore()
      const fetchPromise = store.fetchRooms()

      expect(store.loading).toBe(true)

      resolvePromise({
        data: {
          success: true,
          data: []
        }
      })

      await fetchPromise
      expect(store.loading).toBe(false)
    })
  })

  describe('fetchAvailableRooms', () => {
    it('should fetch available rooms and update state', async () => {
      mockedApi.get.mockResolvedValueOnce({
        data: {
          success: true,
          data: [mockRoomAvailability]
        }
      })

      const store = useRoomStore()
      await store.fetchAvailableRooms()

      expect(mockedApi.get).toHaveBeenCalledWith('/v1/rooms/available')
      expect(store.availableRooms).toHaveLength(1)
      expect(store.availableRooms[0]).toEqual(mockRoomAvailability)
      expect(store.availableRooms[0]!.availableBeds).toBe(1)
    })

    it('should return only rooms with available beds', async () => {
      const roomsWithAvailability: RoomAvailability[] = [
        { ...mockRoomAvailability, id: 1, availableBeds: 1 },
        { ...mockRoomAvailability, id: 2, number: '102', availableBeds: 0 },
        {
          ...mockRoomAvailability,
          id: 3,
          number: '201',
          type: RoomType.SHARED,
          capacity: 4,
          availableBeds: 3
        }
      ]

      mockedApi.get.mockResolvedValueOnce({
        data: {
          success: true,
          data: roomsWithAvailability.filter(r => r.availableBeds > 0)
        }
      })

      const store = useRoomStore()
      await store.fetchAvailableRooms()

      expect(store.availableRooms).toHaveLength(2)
      expect(store.availableRooms.every(r => r.availableBeds > 0)).toBe(true)
    })
  })

  describe('fetchRoom', () => {
    it('should fetch a single room and set as current', async () => {
      mockedApi.get.mockResolvedValueOnce({
        data: { success: true, data: mockRoom }
      })

      const store = useRoomStore()
      const result = await store.fetchRoom(1)

      expect(mockedApi.get).toHaveBeenCalledWith('/v1/rooms/1')
      expect(result).toEqual(mockRoom)
      expect(store.currentRoom).toEqual(mockRoom)
    })

    it('should throw error when room not found', async () => {
      mockedApi.get.mockResolvedValueOnce({
        data: { success: false, message: 'Room not found' }
      })

      const store = useRoomStore()

      await expect(store.fetchRoom(999)).rejects.toThrow('Room not found')
    })
  })

  describe('createRoom', () => {
    it('should create room and return data', async () => {
      mockedApi.post.mockResolvedValueOnce({
        data: { success: true, data: mockRoom }
      })

      const store = useRoomStore()
      const createData = {
        number: '101',
        type: RoomType.PRIVATE,
        capacity: 1
      }

      const result = await store.createRoom(createData)

      expect(mockedApi.post).toHaveBeenCalledWith('/v1/rooms', createData)
      expect(result).toEqual(mockRoom)
    })

    it('should throw error on duplicate number', async () => {
      mockedApi.post.mockResolvedValueOnce({
        data: { success: false, message: 'Room with number 101 already exists' }
      })

      const store = useRoomStore()
      const createData = {
        number: '101',
        type: RoomType.PRIVATE,
        capacity: 1
      }

      await expect(store.createRoom(createData)).rejects.toThrow(
        'Room with number 101 already exists'
      )
    })
  })

  describe('updateRoom', () => {
    it('should update room and update current state', async () => {
      const updatedRoom = { ...mockRoom, capacity: 2 }
      mockedApi.put.mockResolvedValueOnce({
        data: { success: true, data: updatedRoom }
      })

      const store = useRoomStore()
      const updateData = {
        number: '101',
        type: RoomType.PRIVATE,
        capacity: 2
      }

      const result = await store.updateRoom(1, updateData)

      expect(mockedApi.put).toHaveBeenCalledWith('/v1/rooms/1', updateData)
      expect(result.capacity).toBe(2)
      expect(store.currentRoom?.capacity).toBe(2)
    })

    it('should throw error when room not found', async () => {
      mockedApi.put.mockResolvedValueOnce({
        data: { success: false, message: 'Room not found' }
      })

      const store = useRoomStore()
      const updateData = {
        number: '101',
        type: RoomType.PRIVATE,
        capacity: 2
      }

      await expect(store.updateRoom(999, updateData)).rejects.toThrow('Room not found')
    })
  })

  describe('deleteRoom', () => {
    it('should delete room', async () => {
      mockedApi.delete.mockResolvedValueOnce({
        data: { success: true }
      })

      const store = useRoomStore()
      await store.deleteRoom(1)

      expect(mockedApi.delete).toHaveBeenCalledWith('/v1/rooms/1')
    })

    it('should throw error when room has active admissions', async () => {
      mockedApi.delete.mockResolvedValueOnce({
        data: { success: false, message: 'Cannot delete room with active admissions' }
      })

      const store = useRoomStore()

      await expect(store.deleteRoom(1)).rejects.toThrow('Cannot delete room with active admissions')
    })
  })

  describe('clearCurrentRoom', () => {
    it('should clear current room', () => {
      const store = useRoomStore()
      store.currentRoom = mockRoom

      expect(store.currentRoom).not.toBeNull()

      store.clearCurrentRoom()

      expect(store.currentRoom).toBeNull()
    })
  })

  describe('room types', () => {
    it('should handle PRIVATE room type', async () => {
      const privateRoom = { ...mockRoom, type: RoomType.PRIVATE }
      mockedApi.get.mockResolvedValueOnce({
        data: { success: true, data: [privateRoom] }
      })

      const store = useRoomStore()
      await store.fetchRooms()

      expect(store.rooms[0]!.type).toBe(RoomType.PRIVATE)
    })

    it('should handle SHARED room type', async () => {
      const sharedRoom = { ...mockRoom, type: RoomType.SHARED, capacity: 4 }
      mockedApi.get.mockResolvedValueOnce({
        data: { success: true, data: [sharedRoom] }
      })

      const store = useRoomStore()
      await store.fetchRooms()

      expect(store.rooms[0]!.type).toBe(RoomType.SHARED)
      expect(store.rooms[0]!.capacity).toBe(4)
    })
  })
})
