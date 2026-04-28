package com.insidehealthgt.hms.dto.request

import com.insidehealthgt.hms.entity.EmergencyAuthorizationReason
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

data class EmergencyAuthorizeMedicalOrderRequest(
    @field:NotNull(message = "Reason is required")
    val reason: EmergencyAuthorizationReason? = null,

    @field:Size(max = 500, message = "Reason note must not exceed 500 characters")
    val reasonNote: String? = null,
)
