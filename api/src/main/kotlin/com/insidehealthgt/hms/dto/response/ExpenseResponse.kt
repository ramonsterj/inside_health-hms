package com.insidehealthgt.hms.dto.response

import com.insidehealthgt.hms.entity.Expense
import com.insidehealthgt.hms.entity.ExpenseCategory
import com.insidehealthgt.hms.entity.ExpenseStatus
import com.insidehealthgt.hms.entity.User
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

data class ExpenseResponse(
    val id: Long,
    val supplierName: String,
    val category: ExpenseCategory,
    val description: String?,
    val amount: BigDecimal,
    val expenseDate: LocalDate,
    val invoiceNumber: String,
    val invoiceDocumentPath: String?,
    val status: ExpenseStatus,
    val isOverdue: Boolean,
    val dueDate: LocalDate?,
    val paidAmount: BigDecimal,
    val remainingAmount: BigDecimal,
    val notes: String?,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?,
    val createdBy: UserSummaryResponse?,
    val updatedBy: UserSummaryResponse?,
) {
    companion object {
        fun from(expense: Expense, createdByUser: User? = null, updatedByUser: User? = null): ExpenseResponse {
            val remaining = expense.amount.subtract(expense.paidAmount)
            val overdue = expense.status != ExpenseStatus.PAID &&
                expense.status != ExpenseStatus.CANCELLED &&
                expense.dueDate != null &&
                LocalDate.now().isAfter(expense.dueDate)
            return ExpenseResponse(
                id = expense.id!!,
                supplierName = expense.supplierName,
                category = expense.category,
                description = expense.description,
                amount = expense.amount,
                expenseDate = expense.expenseDate,
                invoiceNumber = expense.invoiceNumber,
                invoiceDocumentPath = expense.invoiceDocumentPath,
                status = expense.status,
                isOverdue = overdue,
                dueDate = expense.dueDate,
                paidAmount = expense.paidAmount,
                remainingAmount = remaining.max(BigDecimal.ZERO),
                notes = expense.notes,
                createdAt = expense.createdAt,
                updatedAt = expense.updatedAt,
                createdBy = createdByUser?.let { UserSummaryResponse.from(it) },
                updatedBy = updatedByUser?.let { UserSummaryResponse.from(it) },
            )
        }
    }
}
