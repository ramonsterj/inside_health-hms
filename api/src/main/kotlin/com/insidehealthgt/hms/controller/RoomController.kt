package com.insidehealthgt.hms.controller

import com.insidehealthgt.hms.dto.request.CreateRoomRequest
import com.insidehealthgt.hms.dto.request.UpdateRoomRequest
import com.insidehealthgt.hms.dto.response.ApiResponse
import com.insidehealthgt.hms.dto.response.RoomAvailabilityResponse
import com.insidehealthgt.hms.dto.response.RoomResponse
import com.insidehealthgt.hms.service.RoomService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/rooms")
class RoomController(private val roomService: RoomService) {

    @GetMapping
    @PreAuthorize("hasAuthority('room:read')")
    fun listRooms(): ResponseEntity<ApiResponse<List<RoomResponse>>> {
        val rooms = roomService.findAll()
        return ResponseEntity.ok(ApiResponse.success(rooms))
    }

    @GetMapping("/available")
    @PreAuthorize("hasAuthority('room:read')")
    fun listAvailableRooms(): ResponseEntity<ApiResponse<List<RoomAvailabilityResponse>>> {
        val rooms = roomService.findAvailableRooms()
        return ResponseEntity.ok(ApiResponse.success(rooms))
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('room:read')")
    fun getRoom(@PathVariable id: Long): ResponseEntity<ApiResponse<RoomResponse>> {
        val room = roomService.getRoom(id)
        return ResponseEntity.ok(ApiResponse.success(room))
    }

    @GetMapping("/{id}/availability")
    @PreAuthorize("hasAuthority('room:read')")
    fun getRoomAvailability(@PathVariable id: Long): ResponseEntity<ApiResponse<RoomAvailabilityResponse>> {
        val availability = roomService.getRoomAvailability(id)
        return ResponseEntity.ok(ApiResponse.success(availability))
    }

    @PostMapping
    @PreAuthorize("hasAuthority('room:create')")
    fun createRoom(@Valid @RequestBody request: CreateRoomRequest): ResponseEntity<ApiResponse<RoomResponse>> {
        val room = roomService.createRoom(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(room))
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('room:update')")
    fun updateRoom(
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateRoomRequest,
    ): ResponseEntity<ApiResponse<RoomResponse>> {
        val room = roomService.updateRoom(id, request)
        return ResponseEntity.ok(ApiResponse.success(room))
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('room:delete')")
    fun deleteRoom(@PathVariable id: Long): ResponseEntity<ApiResponse<Unit>> {
        roomService.deleteRoom(id)
        return ResponseEntity.ok(ApiResponse.success("Room deleted successfully"))
    }
}
