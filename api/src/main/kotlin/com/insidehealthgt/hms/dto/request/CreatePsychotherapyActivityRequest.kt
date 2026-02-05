package com.insidehealthgt.hms.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size

data class CreatePsychotherapyActivityRequest(
    @field:NotNull(message = "Category is required")
    @field:Positive(message = "Category ID must be positive")
    val categoryId: Long,

    @field:NotBlank(message = "Description is required")
    @field:Size(max = 2000, message = "Description must not exceed 2000 characters")
    val description: String,
)
