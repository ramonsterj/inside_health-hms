package com.insidehealthgt.hms.dto.request

import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.math.BigDecimal

data class CreatePsychotherapyCategoryRequest(
    @field:NotBlank(message = "Name is required")
    @field:Size(max = 100, message = "Name must not exceed 100 characters")
    val name: String,

    @field:Size(max = 255, message = "Description must not exceed 255 characters")
    val description: String? = null,

    val displayOrder: Int = 0,

    val active: Boolean = true,

    @field:DecimalMin(value = "0", message = "Price cannot be negative")
    val price: BigDecimal? = null,

    @field:DecimalMin(value = "0", message = "Cost cannot be negative")
    val cost: BigDecimal? = null,
)
