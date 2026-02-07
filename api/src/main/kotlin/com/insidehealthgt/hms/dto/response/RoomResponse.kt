package com.insidehealthgt.hms.dto.response

import com.insidehealthgt.hms.entity.Room
import com.insidehealthgt.hms.entity.RoomGender
import com.insidehealthgt.hms.entity.RoomType
import com.insidehealthgt.hms.entity.User
import java.math.BigDecimal
import java.time.LocalDateTime

data class RoomResponse(
    val id: Long,
    val number: String,
    val type: RoomType,
    val gender: RoomGender,
    val capacity: Int,
    val price: BigDecimal?,
    val cost: BigDecimal?,
    val createdAt: LocalDateTime?,
    val createdBy: UserSummaryResponse?,
    val updatedAt: LocalDateTime?,
    val updatedBy: UserSummaryResponse?,
) {
    companion object {
        fun from(room: Room, createdByUser: User? = null, updatedByUser: User? = null) = RoomResponse(
            id = room.id!!,
            number = room.number,
            type = room.type,
            gender = room.gender,
            capacity = room.capacity,
            price = room.price,
            cost = room.cost,
            createdAt = room.createdAt,
            createdBy = createdByUser?.let { UserSummaryResponse.from(it) },
            updatedAt = room.updatedAt,
            updatedBy = updatedByUser?.let { UserSummaryResponse.from(it) },
        )

        fun fromSimple(room: Room) = RoomResponse(
            id = room.id!!,
            number = room.number,
            type = room.type,
            gender = room.gender,
            capacity = room.capacity,
            price = room.price,
            cost = room.cost,
            createdAt = room.createdAt,
            createdBy = null,
            updatedAt = room.updatedAt,
            updatedBy = null,
        )
    }
}
