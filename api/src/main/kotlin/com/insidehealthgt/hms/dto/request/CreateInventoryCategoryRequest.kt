package com.insidehealthgt.hms.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class CreateInventoryCategoryRequest(
    @field:NotBlank(message = "Name is required")
    @field:Size(max = 100, message = "Name must not exceed 100 characters")
    val name: String,

    @field:Size(max = 255, message = "Description must not exceed 255 characters")
    val description: String? = null,

    val displayOrder: Int = 0,

    val active: Boolean = true,
)
