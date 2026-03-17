package com.insidehealthgt.hms.dto.request

import com.insidehealthgt.hms.entity.ExpenseCategory
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.time.LocalDate

data class UpdateExpenseRequest(

    @field:NotBlank(message = "Supplier name is required")
    @field:Size(max = 255, message = "Supplier name must not exceed 255 characters")
    val supplierName: String,

    @field:NotNull(message = "Category is required")
    val category: ExpenseCategory,

    val description: String? = null,

    @field:NotNull(message = "Amount is required")
    @field:Positive(message = "Amount must be positive")
    val amount: BigDecimal,

    @field:NotNull(message = "Expense date is required")
    val expenseDate: LocalDate,

    @field:NotBlank(message = "Invoice number is required")
    @field:Size(max = 100, message = "Invoice number must not exceed 100 characters")
    val invoiceNumber: String,

    val dueDate: LocalDate? = null,

    val notes: String? = null,
)
