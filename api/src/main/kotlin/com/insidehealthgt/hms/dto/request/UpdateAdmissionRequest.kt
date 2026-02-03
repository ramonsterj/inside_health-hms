package com.insidehealthgt.hms.dto.request

import com.insidehealthgt.hms.entity.AdmissionType
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

data class UpdateAdmissionRequest(
    val triageCodeId: Long? = null,

    val roomId: Long? = null,

    @field:NotNull(message = "{validation.admission.treatingPhysicianId.required}")
    val treatingPhysicianId: Long,

    val type: AdmissionType? = null,

    @field:Size(max = 2000, message = "{validation.admission.inventory.max}")
    val inventory: String? = null,
)
