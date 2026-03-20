package com.insidehealthgt.hms.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class AcknowledgeRowRequest(

    @field:NotBlank(message = "Reason is required")
    @field:Size(max = 255, message = "Reason must not exceed 255 characters")
    val reason: String,

    val nonLedger: Boolean = false,
)
