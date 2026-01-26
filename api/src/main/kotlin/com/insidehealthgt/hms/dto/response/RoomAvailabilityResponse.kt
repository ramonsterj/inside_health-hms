package com.insidehealthgt.hms.dto.response

import com.insidehealthgt.hms.entity.Room
import com.insidehealthgt.hms.entity.RoomType

data class RoomAvailabilityResponse(
    val id: Long,
    val number: String,
    val type: RoomType,
    val capacity: Int,
    val availableBeds: Int,
) {
    companion object {
        fun from(room: Room, activeAdmissions: Long) = RoomAvailabilityResponse(
            id = room.id!!,
            number = room.number,
            type = room.type,
            capacity = room.capacity,
            availableBeds = (room.capacity - activeAdmissions.toInt()).coerceAtLeast(0),
        )
    }
}
