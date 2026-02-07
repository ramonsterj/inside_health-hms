package com.insidehealthgt.hms.dto.request

import com.insidehealthgt.hms.entity.RoomGender
import com.insidehealthgt.hms.entity.RoomType
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.math.BigDecimal

data class CreateRoomRequest(
    @field:NotBlank(message = "{validation.room.number.required}")
    @field:Size(max = 50, message = "{validation.room.number.max}")
    val number: String,

    @field:NotNull(message = "{validation.room.type.required}")
    val type: RoomType,

    @field:NotNull(message = "{validation.room.gender.required}")
    val gender: RoomGender,

    @field:Min(value = 1, message = "{validation.room.capacity.min}")
    val capacity: Int = 1,

    @field:DecimalMin(value = "0", message = "Price must be greater than or equal to 0")
    val price: BigDecimal? = null,

    @field:DecimalMin(value = "0", message = "Cost must be greater than or equal to 0")
    val cost: BigDecimal? = null,
)
