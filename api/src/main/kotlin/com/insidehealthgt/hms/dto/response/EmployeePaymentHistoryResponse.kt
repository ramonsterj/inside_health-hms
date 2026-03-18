package com.insidehealthgt.hms.dto.response

import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

enum class EmployeePaymentType {
    PAYROLL_ENTRY,
    CONTRACTOR_PAYMENT,
    DOCTOR_FEE_SETTLEMENT,
}

data class EmployeePaymentHistoryResponse(
    val type: EmployeePaymentType,
    val amount: BigDecimal,
    val date: LocalDate,
    val reference: String?,
    val status: String,
    val relatedEntityId: Long,
    val createdAt: LocalDateTime?,
)
