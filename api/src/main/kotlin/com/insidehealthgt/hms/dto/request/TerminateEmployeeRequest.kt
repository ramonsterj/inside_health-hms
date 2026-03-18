package com.insidehealthgt.hms.dto.request

import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.LocalDate

data class TerminateEmployeeRequest(

    @field:NotNull
    val terminationDate: LocalDate,

    @field:Size(max = 255)
    val terminationReason: String? = null,

    val cancelPendingPayroll: Boolean? = false,
)
