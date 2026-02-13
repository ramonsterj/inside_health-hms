package com.insidehealthgt.hms.dto.request

import com.insidehealthgt.hms.entity.AdministrationStatus
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

data class CreateMedicationAdministrationRequest(
    @field:NotNull(message = "Status is required")
    val status: AdministrationStatus,

    @field:Size(max = 1000, message = "Notes must not exceed 1000 characters")
    val notes: String? = null,
)
