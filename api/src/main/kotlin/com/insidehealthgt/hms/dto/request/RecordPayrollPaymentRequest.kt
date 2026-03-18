package com.insidehealthgt.hms.dto.request

import jakarta.validation.constraints.NotNull
import java.time.LocalDate

data class RecordPayrollPaymentRequest(

    @field:NotNull
    val paymentDate: LocalDate,

    @field:NotNull
    val bankAccountId: Long,

    val notes: String? = null,
)
