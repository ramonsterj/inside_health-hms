package com.insidehealthgt.hms.repository

import com.insidehealthgt.hms.entity.ExpensePayment
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ExpensePaymentRepository : JpaRepository<ExpensePayment, Long> {

    fun findAllByExpenseIdOrderByPaymentDateAsc(expenseId: Long): List<ExpensePayment>

    fun findAllByBankAccountIdAndPaymentDateBetween(
        bankAccountId: Long,
        from: java.time.LocalDate,
        to: java.time.LocalDate,
    ): List<ExpensePayment>

    fun findTop10ByBankAccountIdOrderByPaymentDateDesc(bankAccountId: Long): List<ExpensePayment>

    fun findAllByPaymentDateBetween(from: java.time.LocalDate, to: java.time.LocalDate): List<ExpensePayment>
}
