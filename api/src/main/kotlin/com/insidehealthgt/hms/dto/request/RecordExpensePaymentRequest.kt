package com.insidehealthgt.hms.dto.request

import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.time.LocalDate

data class RecordExpensePaymentRequest(

    @field:NotNull(message = "Amount is required")
    @field:Positive(message = "Amount must be positive")
    val amount: BigDecimal,

    @field:NotNull(message = "Payment date is required")
    val paymentDate: LocalDate,

    @field:NotNull(message = "Bank account is required")
    val bankAccountId: Long,

    @field:Size(max = 255, message = "Reference must not exceed 255 characters")
    val reference: String? = null,

    val notes: String? = null,
)
