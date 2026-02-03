package com.insidehealthgt.hms.dto.request

import com.insidehealthgt.hms.entity.AdmissionType
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

data class CreateAdmissionRequest(
    @field:NotNull(message = "{validation.admission.patientId.required}")
    val patientId: Long,

    val triageCodeId: Long? = null,

    val roomId: Long? = null,

    @field:NotNull(message = "{validation.admission.treatingPhysicianId.required}")
    val treatingPhysicianId: Long,

    @field:NotNull(message = "{validation.admission.admissionDate.required}")
    val admissionDate: LocalDateTime,

    @field:NotNull(message = "{validation.admission.type.required}")
    val type: AdmissionType,

    @field:Size(max = 2000, message = "{validation.admission.inventory.max}")
    val inventory: String? = null,
)
