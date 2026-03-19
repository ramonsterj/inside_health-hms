package com.insidehealthgt.hms.dto.request

import com.insidehealthgt.hms.entity.IncomeCategory
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.time.LocalDate

data class CreateIncomeFromRowRequest(

    @field:NotBlank(message = "Description is required")
    @field:Size(max = 255, message = "Description must not exceed 255 characters")
    val description: String,

    @field:NotNull(message = "Category is required")
    val category: IncomeCategory,

    @field:NotNull(message = "Amount is required")
    @field:Positive(message = "Amount must be positive")
    val amount: BigDecimal,

    @field:NotNull(message = "Income date is required")
    val incomeDate: LocalDate,

    val reference: String? = null,

    val notes: String? = null,
)
