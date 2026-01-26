package com.insidehealthgt.hms.dto.request

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class UpdateTriageCodeRequest(
    @field:NotBlank(message = "{validation.triageCode.code.required}")
    @field:Size(max = 10, message = "{validation.triageCode.code.max}")
    val code: String,

    @field:NotBlank(message = "{validation.triageCode.color.required}")
    @field:Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "{validation.triageCode.color.invalid}")
    val color: String,

    @field:Size(max = 255, message = "{validation.triageCode.description.max}")
    val description: String? = null,

    @field:Min(value = 0, message = "{validation.triageCode.displayOrder.min}")
    val displayOrder: Int = 0,
)
