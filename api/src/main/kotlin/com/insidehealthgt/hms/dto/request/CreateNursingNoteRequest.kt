package com.insidehealthgt.hms.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class CreateNursingNoteRequest(
    @field:NotBlank(message = "Description is required")
    @field:Size(max = 5000, message = "Description must be at most 5000 characters")
    val description: String,
)

typealias UpdateNursingNoteRequest = CreateNursingNoteRequest
