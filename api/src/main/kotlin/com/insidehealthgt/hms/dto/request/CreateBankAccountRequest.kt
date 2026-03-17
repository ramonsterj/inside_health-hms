package com.insidehealthgt.hms.dto.request

import com.insidehealthgt.hms.entity.BankAccountType
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.PositiveOrZero
import jakarta.validation.constraints.Size
import java.math.BigDecimal

data class CreateBankAccountRequest(

    @field:NotBlank(message = "Account name is required")
    @field:Size(max = 100, message = "Name must not exceed 100 characters")
    val name: String,

    @field:Size(max = 100, message = "Bank name must not exceed 100 characters")
    val bankName: String? = null,

    @field:Size(max = 50, message = "Account number must not exceed 50 characters")
    val accountNumber: String? = null,

    @field:NotNull(message = "Account type is required")
    val accountType: BankAccountType,

    @field:Size(min = 3, max = 3, message = "Currency must be a 3-letter code")
    val currency: String = "GTQ",

    @field:PositiveOrZero(message = "Opening balance must be zero or positive")
    val openingBalance: BigDecimal = BigDecimal.ZERO,

    val notes: String? = null,
)
