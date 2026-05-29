package com.insidehealthgt.hms.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class CreateWarehouseRequest(
    @field:NotBlank(message = "Code is required")
    @field:Size(max = 50, message = "Code must not exceed 50 characters")
    @field:Pattern(
        regexp = "^[A-Z0-9_]+$",
        message = "Code must be uppercase letters, digits and underscores only",
    )
    val code: String,

    @field:NotBlank(message = "Name is required")
    @field:Size(max = 255, message = "Name must not exceed 255 characters")
    val name: String,

    @field:Size(max = 2000, message = "Description must not exceed 2000 characters")
    val description: String? = null,

    val active: Boolean = true,
)
