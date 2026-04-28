package com.insidehealthgt.hms.dto.request

import jakarta.validation.constraints.Size

data class RejectMedicalOrderRequest(
    @field:Size(max = 500, message = "Reason must not exceed 500 characters")
    val reason: String? = null,
)
