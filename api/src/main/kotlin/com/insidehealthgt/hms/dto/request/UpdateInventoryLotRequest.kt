package com.insidehealthgt.hms.dto.request

import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.LocalDate

data class UpdateInventoryLotRequest(
    @field:Size(max = 50)
    val lotNumber: String? = null,

    @field:NotNull
    val expirationDate: LocalDate,

    @field:Size(max = 150)
    val supplier: String? = null,

    @field:Size(max = 500)
    val notes: String? = null,

    val recalled: Boolean = false,

    @field:Size(max = 500)
    val recalledReason: String? = null,
)
