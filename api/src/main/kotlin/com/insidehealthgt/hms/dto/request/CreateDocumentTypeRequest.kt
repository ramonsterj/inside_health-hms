package com.insidehealthgt.hms.dto.request

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class CreateDocumentTypeRequest(
    @field:NotBlank(message = "{validation.documentType.code.required}")
    @field:Size(max = 50, message = "{validation.documentType.code.max}")
    @field:Pattern(regexp = "^[A-Z_]+$", message = "{validation.documentType.code.pattern}")
    val code: String,

    @field:NotBlank(message = "{validation.documentType.name.required}")
    @field:Size(max = 100, message = "{validation.documentType.name.max}")
    val name: String,

    @field:Size(max = 255, message = "{validation.documentType.description.max}")
    val description: String? = null,

    @field:Min(value = 0, message = "{validation.documentType.displayOrder.min}")
    val displayOrder: Int = 0,
)
