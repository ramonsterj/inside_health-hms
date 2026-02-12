package com.insidehealthgt.hms.dto.request

import jakarta.validation.constraints.Negative
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

data class CreateAdjustmentRequest(
    @field:NotBlank(message = "Description is required")
    @field:Size(max = 500, message = "Description must not exceed 500 characters")
    val description: String,

    @field:NotNull(message = "Amount is required")
    @field:Negative(message = "Adjustment amount must be negative")
    val amount: java.math.BigDecimal,

    @field:NotBlank(message = "Reason is required for adjustments")
    @field:Size(max = 500, message = "Reason must not exceed 500 characters")
    val reason: String,
)
