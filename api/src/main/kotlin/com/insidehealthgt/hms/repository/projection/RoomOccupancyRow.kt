package com.insidehealthgt.hms.repository.projection

import com.insidehealthgt.hms.entity.RoomGender
import com.insidehealthgt.hms.entity.RoomType
import java.time.LocalDateTime

/**
 * Flat projection row returned by [com.insidehealthgt.hms.repository.RoomRepository.findRoomsWithActiveAdmissions].
 * One row per (room, occupant) — rooms with no occupants appear once with null occupant fields.
 */
data class RoomOccupancyRow(
    val roomId: Long,
    val roomNumber: String,
    val roomType: RoomType,
    val roomGender: RoomGender,
    val capacity: Int,
    val admissionId: Long?,
    val patientId: Long?,
    val patientFirstName: String?,
    val patientLastName: String?,
    val admissionDate: LocalDateTime?,
)
