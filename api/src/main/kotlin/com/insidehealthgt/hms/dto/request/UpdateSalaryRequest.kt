package com.insidehealthgt.hms.dto.request

import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import java.math.BigDecimal
import java.time.LocalDate

data class UpdateSalaryRequest(

    @field:NotNull
    @field:Positive
    val newSalary: BigDecimal,

    @field:NotNull
    val effectiveFrom: LocalDate,

    val notes: String? = null,
)
