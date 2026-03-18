package com.insidehealthgt.hms.dto.response

import com.insidehealthgt.hms.entity.PayrollEntry
import com.insidehealthgt.hms.entity.PayrollPeriod
import com.insidehealthgt.hms.entity.PayrollStatus
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

data class PayrollEntryResponse(
    val id: Long,
    val employeeId: Long,
    val employeeName: String,
    val year: Int,
    val period: PayrollPeriod,
    val periodLabel: String,
    val baseSalary: BigDecimal,
    val grossAmount: BigDecimal,
    val dueDate: LocalDate,
    val status: PayrollStatus,
    val paidDate: LocalDate?,
    val expenseId: Long?,
    val notes: String?,
    val createdAt: LocalDateTime?,
) {
    companion object {
        fun from(entry: PayrollEntry): PayrollEntryResponse = PayrollEntryResponse(
            id = entry.id!!,
            employeeId = entry.employee.id!!,
            employeeName = entry.employee.fullName,
            year = entry.year,
            period = entry.period,
            periodLabel = entry.periodLabel,
            baseSalary = entry.baseSalary,
            grossAmount = entry.grossAmount,
            dueDate = entry.dueDate,
            status = entry.status,
            paidDate = entry.paidDate,
            expenseId = entry.expenseId,
            notes = entry.notes,
            createdAt = entry.createdAt,
        )
    }
}
