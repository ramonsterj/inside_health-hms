package com.insidehealthgt.hms.dto.response

import com.insidehealthgt.hms.entity.Income
import com.insidehealthgt.hms.entity.IncomeCategory
import com.insidehealthgt.hms.entity.User
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

data class IncomeResponse(
    val id: Long,
    val description: String,
    val category: IncomeCategory,
    val amount: BigDecimal,
    val incomeDate: LocalDate,
    val reference: String?,
    val bankAccountId: Long,
    val bankAccountName: String,
    val invoiceId: Long?,
    val invoiceNumber: String?,
    val notes: String?,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?,
    val createdBy: UserSummaryResponse?,
    val updatedBy: UserSummaryResponse?,
) {
    companion object {
        fun from(
            income: Income,
            invoiceNumber: String? = null,
            createdByUser: User? = null,
            updatedByUser: User? = null,
        ): IncomeResponse = IncomeResponse(
            id = income.id!!,
            description = income.description,
            category = income.category,
            amount = income.amount,
            incomeDate = income.incomeDate,
            reference = income.reference,
            bankAccountId = income.bankAccount.id!!,
            bankAccountName = income.bankAccount.name,
            invoiceId = income.invoiceId,
            invoiceNumber = invoiceNumber,
            notes = income.notes,
            createdAt = income.createdAt,
            updatedAt = income.updatedAt,
            createdBy = createdByUser?.let { UserSummaryResponse.from(it) },
            updatedBy = updatedByUser?.let { UserSummaryResponse.from(it) },
        )
    }
}
