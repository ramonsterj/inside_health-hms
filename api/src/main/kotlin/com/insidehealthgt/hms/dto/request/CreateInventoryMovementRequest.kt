package com.insidehealthgt.hms.dto.request

import com.insidehealthgt.hms.entity.MovementType
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size

data class CreateInventoryMovementRequest(
    @field:NotNull(message = "Movement type is required")
    val type: MovementType,

    @field:NotNull(message = "Quantity is required")
    @field:Positive(message = "Quantity must be greater than 0")
    val quantity: Int,

    @field:Size(max = 500, message = "Notes must not exceed 500 characters")
    val notes: String? = null,
)
