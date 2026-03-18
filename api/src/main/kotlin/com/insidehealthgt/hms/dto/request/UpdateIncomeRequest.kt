package com.insidehealthgt.hms.dto.request

import com.insidehealthgt.hms.entity.IncomeCategory
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.time.LocalDate

data class UpdateIncomeRequest(

    @field:NotBlank
    @field:Size(max = 255)
    val description: String,

    @field:NotNull
    val category: IncomeCategory,

    @field:NotNull
    @field:Positive
    val amount: BigDecimal,

    @field:NotNull
    val incomeDate: LocalDate,

    @field:Size(max = 100)
    val reference: String? = null,

    @field:NotNull
    val bankAccountId: Long,

    val invoiceId: Long? = null,

    val notes: String? = null,
)
