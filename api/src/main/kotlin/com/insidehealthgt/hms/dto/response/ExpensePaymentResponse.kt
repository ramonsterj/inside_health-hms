package com.insidehealthgt.hms.dto.response

import com.insidehealthgt.hms.entity.ExpensePayment
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

data class ExpensePaymentResponse(
    val id: Long,
    val expenseId: Long,
    val amount: BigDecimal,
    val paymentDate: LocalDate,
    val bankAccountId: Long,
    val bankAccountName: String,
    val maskedAccountNumber: String?,
    val reference: String?,
    val notes: String?,
    val createdAt: LocalDateTime?,
) {
    companion object {
        fun from(payment: ExpensePayment): ExpensePaymentResponse = ExpensePaymentResponse(
            id = payment.id!!,
            expenseId = payment.expense.id!!,
            amount = payment.amount,
            paymentDate = payment.paymentDate,
            bankAccountId = payment.bankAccount.id!!,
            bankAccountName = payment.bankAccount.name,
            maskedAccountNumber = payment.bankAccount.accountNumber?.let { num ->
                BankAccountResponse.maskAccountNumber(num)
            },
            reference = payment.reference,
            notes = payment.notes,
            createdAt = payment.createdAt,
        )
    }
}
