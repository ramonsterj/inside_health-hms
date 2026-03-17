package com.insidehealthgt.hms.repository

import com.insidehealthgt.hms.entity.ExpensePayment
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ExpensePaymentRepository : JpaRepository<ExpensePayment, Long> {

    fun findAllByExpenseIdOrderByPaymentDateAsc(expenseId: Long): List<ExpensePayment>
}
