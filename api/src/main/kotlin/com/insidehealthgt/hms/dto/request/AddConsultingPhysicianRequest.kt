package com.insidehealthgt.hms.dto.request

import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.LocalDate

data class AddConsultingPhysicianRequest(
    @field:NotNull(message = "{validation.consultingPhysician.physicianId.required}")
    val physicianId: Long,

    @field:Size(max = 500, message = "{validation.consultingPhysician.reason.max}")
    val reason: String? = null,

    val requestedDate: LocalDate? = null,
)
