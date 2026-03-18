package com.insidehealthgt.hms.dto.response

import com.insidehealthgt.hms.entity.SalaryHistory
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

data class SalaryHistoryResponse(
    val id: Long,
    val employeeId: Long,
    val baseSalary: BigDecimal,
    val effectiveFrom: LocalDate,
    val effectiveTo: LocalDate?,
    val notes: String?,
    val createdAt: LocalDateTime?,
) {
    companion object {
        fun from(history: SalaryHistory): SalaryHistoryResponse = SalaryHistoryResponse(
            id = history.id!!,
            employeeId = history.employee.id!!,
            baseSalary = history.baseSalary,
            effectiveFrom = history.effectiveFrom,
            effectiveTo = history.effectiveTo,
            notes = history.notes,
            createdAt = history.createdAt,
        )
    }
}
