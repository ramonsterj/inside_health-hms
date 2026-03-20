package com.insidehealthgt.hms.repository

import com.insidehealthgt.hms.entity.Expense
import com.insidehealthgt.hms.entity.ExpenseStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
interface ExpenseRepository :
    JpaRepository<Expense, Long>,
    JpaSpecificationExecutor<Expense> {

    fun findAllByStatusInAndDueDateBetween(statuses: List<ExpenseStatus>, from: LocalDate, to: LocalDate): List<Expense>

    fun findAllByExpenseDateBetween(from: LocalDate, to: LocalDate): List<Expense>
}
