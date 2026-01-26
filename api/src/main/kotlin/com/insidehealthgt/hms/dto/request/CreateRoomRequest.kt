package com.insidehealthgt.hms.dto.request

import com.insidehealthgt.hms.entity.RoomType
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

data class CreateRoomRequest(
    @field:NotBlank(message = "{validation.room.number.required}")
    @field:Size(max = 50, message = "{validation.room.number.max}")
    val number: String,

    @field:NotNull(message = "{validation.room.type.required}")
    val type: RoomType,

    @field:Min(value = 1, message = "{validation.room.capacity.min}")
    val capacity: Int = 1,
)
