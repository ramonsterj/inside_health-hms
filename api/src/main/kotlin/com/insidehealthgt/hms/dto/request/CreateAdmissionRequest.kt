package com.insidehealthgt.hms.dto.request

import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

data class CreateAdmissionRequest(
    @field:NotNull(message = "{validation.admission.patientId.required}")
    val patientId: Long,

    @field:NotNull(message = "{validation.admission.triageCodeId.required}")
    val triageCodeId: Long,

    @field:NotNull(message = "{validation.admission.roomId.required}")
    val roomId: Long,

    @field:NotNull(message = "{validation.admission.treatingPhysicianId.required}")
    val treatingPhysicianId: Long,

    @field:NotNull(message = "{validation.admission.admissionDate.required}")
    val admissionDate: LocalDateTime,

    @field:Size(max = 2000, message = "{validation.admission.inventory.max}")
    val inventory: String? = null,
)
