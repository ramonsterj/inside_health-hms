package com.insidehealthgt.hms.dto.request

import com.insidehealthgt.hms.entity.AdministrationStatus
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

data class CreateMedicationAdministrationRequest(
    @field:NotNull(message = "Status is required")
    val status: AdministrationStatus,

    @field:Size(max = 1000, message = "Notes must not exceed 1000 characters")
    val notes: String? = null,

    @field:Min(value = 1, message = "Quantity must be at least 1")
    val quantity: Int = 1,

    /** ADMIN-only override of FEFO lot selection. Non-admin senders receive 403. */
    val lotId: Long? = null,
)
