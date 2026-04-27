import { describe, it, expect, beforeEach, vi, type Mock } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useBedOccupancyStore } from './bedOccupancy'
import api from '@/services/api'
import { RoomGender, RoomType, type BedOccupancyResponse } from '@/types/room'

vi.mock('@/services/api', () => ({
  default: {
    get: vi.fn()
  }
}))

const mockedApi = api as unknown as { get: Mock }

const mockResponse: BedOccupancyResponse = {
  summary: {
    totalBeds: 4,
    occupiedBeds: 1,
    freeBeds: 3,
    occupancyPercent: 25.0
  },
  rooms: [
    {
      id: 1,
      number: '101',
      type: RoomType.PRIVATE,
      gender: RoomGender.FEMALE,
      capacity: 1,
      occupiedBeds: 1,
      availableBeds: 0,
      occupants: [
        {
          admissionId: 100,
          patientId: 50,
          patientName: 'María González',
          admissionDate: '2026-04-22'
        }
      ]
    },
    {
      id: 2,
      number: '202',
      type: RoomType.SHARED,
      gender: RoomGender.MALE,
      capacity: 3,
      occupiedBeds: 0,
      availableBeds: 3,
      occupants: []
    }
  ]
}

describe('useBedOccupancyStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('starts in an empty initial state', () => {
    const store = useBedOccupancyStore()
    expect(store.rooms).toHaveLength(0)
    expect(store.summary.totalBeds).toBe(0)
    expect(store.loading).toBe(false)
    expect(store.error).toBe(null)
    expect(store.lastFetchedAt).toBe(null)
    expect(store.hasLoadedOnce).toBe(false)
  })

  it('populates rooms and summary on successful fetch', async () => {
    mockedApi.get.mockResolvedValueOnce({ data: { success: true, data: mockResponse } })
    const store = useBedOccupancyStore()

    await store.fetchOccupancy()

    expect(mockedApi.get).toHaveBeenCalledWith('/v1/rooms/occupancy')
    expect(store.rooms).toHaveLength(2)
    expect(store.summary.totalBeds).toBe(4)
    expect(store.summary.occupiedBeds).toBe(1)
    expect(store.error).toBe(null)
    expect(store.hasLoadedOnce).toBe(true)
    expect(store.lastFetchedAt).toBeInstanceOf(Date)
  })

  it('keeps prior data and surfaces error on failed refresh', async () => {
    mockedApi.get.mockResolvedValueOnce({ data: { success: true, data: mockResponse } })
    const store = useBedOccupancyStore()
    await store.fetchOccupancy()

    mockedApi.get.mockRejectedValueOnce(new Error('Network error'))
    await expect(store.fetchOccupancy()).rejects.toThrow('Network error')

    expect(store.rooms).toHaveLength(2) // prior payload retained
    expect(store.error).toBe('Network error')
    expect(store.hasLoadedOnce).toBe(true)
  })

  it('reports error when API returns success=false', async () => {
    mockedApi.get.mockResolvedValueOnce({
      data: { success: false, message: 'Server failure' }
    })
    const store = useBedOccupancyStore()

    await expect(store.fetchOccupancy()).rejects.toThrow('Server failure')
    expect(store.error).toBe('Server failure')
    expect(store.hasLoadedOnce).toBe(false)
  })

  it('reset clears all state', async () => {
    mockedApi.get.mockResolvedValueOnce({ data: { success: true, data: mockResponse } })
    const store = useBedOccupancyStore()
    await store.fetchOccupancy()

    store.reset()

    expect(store.rooms).toHaveLength(0)
    expect(store.summary.totalBeds).toBe(0)
    expect(store.lastFetchedAt).toBe(null)
    expect(store.hasLoadedOnce).toBe(false)
    expect(store.error).toBe(null)
  })
})
