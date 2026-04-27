package com.insidehealthgt.hms.service

import com.insidehealthgt.hms.dto.request.CreateRoomRequest
import com.insidehealthgt.hms.dto.request.UpdateRoomRequest
import com.insidehealthgt.hms.dto.response.BedOccupancyResponse
import com.insidehealthgt.hms.dto.response.BedOccupant
import com.insidehealthgt.hms.dto.response.OccupancySummary
import com.insidehealthgt.hms.dto.response.RoomAvailabilityResponse
import com.insidehealthgt.hms.dto.response.RoomOccupancyItem
import com.insidehealthgt.hms.dto.response.RoomResponse
import com.insidehealthgt.hms.entity.Room
import com.insidehealthgt.hms.exception.BadRequestException
import com.insidehealthgt.hms.exception.ResourceNotFoundException
import com.insidehealthgt.hms.repository.AdmissionRepository
import com.insidehealthgt.hms.repository.RoomRepository
import com.insidehealthgt.hms.repository.UserRepository
import com.insidehealthgt.hms.repository.projection.RoomOccupancyRow
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime

@Service
@Suppress("TooManyFunctions")
class RoomService(
    private val roomRepository: RoomRepository,
    private val admissionRepository: AdmissionRepository,
    private val userRepository: UserRepository,
) {

    @Transactional(readOnly = true)
    fun findById(id: Long): Room = roomRepository.findById(id)
        .orElseThrow { ResourceNotFoundException("Room not found with id: $id") }

    @Transactional(readOnly = true)
    fun findAll(): List<RoomResponse> = roomRepository.findAll()
        .map { RoomResponse.fromSimple(it) }

    @Transactional(readOnly = true)
    fun findAvailableRooms(): List<RoomAvailabilityResponse> {
        val roomsWithCounts = roomRepository.findRoomsWithAvailabilityAndCount()
        return roomsWithCounts.map { row ->
            val room = row[0] as Room
            val activeAdmissions = row[1] as Long
            RoomAvailabilityResponse.from(room, activeAdmissions)
        }
    }

    @Transactional(readOnly = true)
    fun getRoom(id: Long): RoomResponse {
        val room = findById(id)
        return buildRoomResponse(room)
    }

    @Transactional(readOnly = true)
    fun getRoomAvailability(id: Long): RoomAvailabilityResponse {
        val room = findById(id)
        val activeAdmissions = roomRepository.countActiveAdmissionsByRoomId(id)
        return RoomAvailabilityResponse.from(room, activeAdmissions)
    }

    @Transactional(readOnly = true)
    fun getAvailableBeds(roomId: Long): Int {
        val room = findById(roomId)
        val activeAdmissions = roomRepository.countActiveAdmissionsByRoomId(roomId)
        return (room.capacity - activeAdmissions.toInt()).coerceAtLeast(0)
    }

    @Transactional(readOnly = true)
    fun hasAvailableBeds(roomId: Long): Boolean = getAvailableBeds(roomId) > 0

    /**
     * Builds a hospital-wide bed occupancy snapshot in one query.
     *
     * Filter rules:
     * - Soft-deleted rooms are excluded (enforced by `@SQLRestriction` on [Room]).
     * - An admission counts as occupying a bed only when it is `ACTIVE`, not soft-deleted,
     *   and of type `HOSPITALIZATION` (other types do not require a room — see
     *   [com.insidehealthgt.hms.entity.AdmissionType.requiresRoom]).
     */
    @Transactional(readOnly = true)
    fun getBedOccupancy(): BedOccupancyResponse {
        val rows = roomRepository.findRoomsWithActiveAdmissions()
        val rooms = rows.groupBy { it.roomId }
            .map { (_, roomRows) -> toRoomOccupancyItem(roomRows) }
            .sortedBy { it.number }

        val totalBeds = rooms.sumOf { it.capacity }
        val occupiedBeds = rooms.sumOf { it.occupiedBeds }
        val freeBeds = totalBeds - occupiedBeds
        val occupancyPercent = if (totalBeds == 0) {
            0.0
        } else {
            BigDecimal(occupiedBeds * PERCENT_SCALE)
                .divide(BigDecimal(totalBeds), 2, RoundingMode.HALF_UP)
                .toDouble()
        }

        return BedOccupancyResponse(
            summary = OccupancySummary(
                totalBeds = totalBeds,
                occupiedBeds = occupiedBeds,
                freeBeds = freeBeds,
                occupancyPercent = occupancyPercent,
            ),
            rooms = rooms,
        )
    }

    private fun toRoomOccupancyItem(rows: List<RoomOccupancyRow>): RoomOccupancyItem {
        val first = rows.first()
        // Repository query orders by `a.admissionDate`; `groupBy` preserves insertion order.
        val occupants = rows
            .filter { it.admissionId != null }
            .map { row ->
                BedOccupant(
                    admissionId = row.admissionId!!,
                    patientId = row.patientId!!,
                    patientName = "${row.patientFirstName.orEmpty()} ${row.patientLastName.orEmpty()}".trim(),
                    admissionDate = row.admissionDate!!.toLocalDate(),
                )
            }
            .take(first.capacity)
        val occupiedBeds = occupants.size
        return RoomOccupancyItem(
            id = first.roomId,
            number = first.roomNumber,
            type = first.roomType,
            gender = first.roomGender,
            capacity = first.capacity,
            occupiedBeds = occupiedBeds,
            availableBeds = (first.capacity - occupiedBeds).coerceAtLeast(0),
            occupants = occupants,
        )
    }

    private companion object {
        const val PERCENT_SCALE = 100
    }

    @Transactional
    fun createRoom(request: CreateRoomRequest): RoomResponse {
        if (roomRepository.existsByNumber(request.number)) {
            throw BadRequestException("Room with number '${request.number}' already exists")
        }

        val room = Room(
            number = request.number,
            type = request.type,
            gender = request.gender,
            capacity = request.capacity,
            price = request.price,
            cost = request.cost,
        )

        val savedRoom = roomRepository.save(room)
        return buildRoomResponse(savedRoom)
    }

    @Transactional
    fun updateRoom(id: Long, request: UpdateRoomRequest): RoomResponse {
        val room = findById(id)

        if (roomRepository.existsByNumberExcludingId(request.number, id)) {
            throw BadRequestException("Room with number '${request.number}' already exists")
        }

        // Check if reducing capacity below active admissions count
        val activeAdmissions = roomRepository.countActiveAdmissionsByRoomId(id)
        if (request.capacity < activeAdmissions) {
            throw BadRequestException(
                "Cannot reduce capacity to ${request.capacity}. Room has $activeAdmissions active admissions.",
            )
        }

        room.number = request.number
        room.type = request.type
        room.gender = request.gender
        room.capacity = request.capacity
        room.price = request.price
        room.cost = request.cost

        val savedRoom = roomRepository.save(room)
        return buildRoomResponse(savedRoom)
    }

    @Transactional
    fun deleteRoom(id: Long) {
        val room = findById(id)

        if (admissionRepository.existsActiveByRoomId(id)) {
            throw BadRequestException("Cannot delete room that has active admissions")
        }

        room.deletedAt = LocalDateTime.now()
        roomRepository.save(room)
    }

    private fun buildRoomResponse(room: Room): RoomResponse {
        val createdByUser = room.createdBy?.let { userRepository.findById(it).orElse(null) }
        val updatedByUser = room.updatedBy?.let { userRepository.findById(it).orElse(null) }
        return RoomResponse.from(room, createdByUser, updatedByUser)
    }
}
