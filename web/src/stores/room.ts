import { defineStore } from 'pinia'
import { ref } from 'vue'
import api from '@/services/api'
import type { Room, RoomAvailability, CreateRoomRequest, UpdateRoomRequest } from '@/types/room'
import type { ApiResponse } from '@/types'

export const useRoomStore = defineStore('room', () => {
  const rooms = ref<Room[]>([])
  const availableRooms = ref<RoomAvailability[]>([])
  const currentRoom = ref<Room | null>(null)
  const loading = ref(false)

  async function fetchRooms(): Promise<void> {
    loading.value = true
    try {
      const response = await api.get<ApiResponse<Room[]>>('/v1/rooms')
      if (response.data.success && response.data.data) {
        rooms.value = response.data.data
      }
    } finally {
      loading.value = false
    }
  }

  async function fetchAvailableRooms(): Promise<void> {
    loading.value = true
    try {
      const response = await api.get<ApiResponse<RoomAvailability[]>>('/v1/rooms/available')
      if (response.data.success && response.data.data) {
        availableRooms.value = response.data.data
      }
    } finally {
      loading.value = false
    }
  }

  async function fetchRoom(id: number): Promise<Room> {
    loading.value = true
    try {
      const response = await api.get<ApiResponse<Room>>(`/v1/rooms/${id}`)
      if (response.data.success && response.data.data) {
        currentRoom.value = response.data.data
        return response.data.data
      }
      throw new Error(response.data.message || 'Room not found')
    } finally {
      loading.value = false
    }
  }

  async function createRoom(data: CreateRoomRequest): Promise<Room> {
    loading.value = true
    try {
      const response = await api.post<ApiResponse<Room>>('/v1/rooms', data)
      if (response.data.success && response.data.data) {
        return response.data.data
      }
      throw new Error(response.data.message || 'Create room failed')
    } finally {
      loading.value = false
    }
  }

  async function updateRoom(id: number, data: UpdateRoomRequest): Promise<Room> {
    loading.value = true
    try {
      const response = await api.put<ApiResponse<Room>>(`/v1/rooms/${id}`, data)
      if (response.data.success && response.data.data) {
        currentRoom.value = response.data.data
        return response.data.data
      }
      throw new Error(response.data.message || 'Update room failed')
    } finally {
      loading.value = false
    }
  }

  async function deleteRoom(id: number): Promise<void> {
    loading.value = true
    try {
      const response = await api.delete<ApiResponse<void>>(`/v1/rooms/${id}`)
      if (!response.data.success) {
        throw new Error(response.data.message || 'Delete room failed')
      }
    } finally {
      loading.value = false
    }
  }

  function clearCurrentRoom(): void {
    currentRoom.value = null
  }

  return {
    rooms,
    availableRooms,
    currentRoom,
    loading,
    fetchRooms,
    fetchAvailableRooms,
    fetchRoom,
    createRoom,
    updateRoom,
    deleteRoom,
    clearCurrentRoom
  }
})
