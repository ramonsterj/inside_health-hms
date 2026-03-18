package com.insidehealthgt.hms.dto.response

import java.math.BigDecimal
import java.time.LocalDate

data class IndemnizacionResponse(
    val employeeId: Long,
    val employeeName: String,
    val baseSalary: BigDecimal,
    val hireDate: LocalDate,
    val daysWorked: Long,
    val liability: BigDecimal,
    val asOfDate: LocalDate,
)
