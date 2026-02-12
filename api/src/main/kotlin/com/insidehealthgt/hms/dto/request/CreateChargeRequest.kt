package com.insidehealthgt.hms.dto.request

import com.insidehealthgt.hms.entity.ChargeType
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.PositiveOrZero
import jakarta.validation.constraints.Size

data class CreateChargeRequest(
    @field:NotNull(message = "Charge type is required")
    val chargeType: ChargeType,

    @field:NotBlank(message = "Description is required")
    @field:Size(max = 500, message = "Description must not exceed 500 characters")
    val description: String,

    @field:NotNull(message = "Quantity is required")
    @field:Positive(message = "Quantity must be greater than 0")
    val quantity: Int,

    @field:NotNull(message = "Unit price is required")
    @field:PositiveOrZero(message = "Unit price must be 0 or greater")
    val unitPrice: java.math.BigDecimal,

    val inventoryItemId: Long? = null,
)
