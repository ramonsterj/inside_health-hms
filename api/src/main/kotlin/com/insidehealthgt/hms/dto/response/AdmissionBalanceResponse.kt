package com.insidehealthgt.hms.dto.response

import com.insidehealthgt.hms.entity.ChargeType
import java.math.BigDecimal
import java.time.LocalDate

data class AdmissionBalanceResponse(
    val admissionId: Long,
    val patientName: String,
    val admissionDate: LocalDate,
    val totalBalance: BigDecimal,
    val dailyBreakdown: List<DailyChargeGroup>,
)

data class DailyChargeGroup(
    val date: LocalDate,
    val charges: List<DailyChargeItem>,
    val dailyTotal: BigDecimal,
    val cumulativeTotal: BigDecimal,
)

data class DailyChargeItem(
    val id: Long,
    val chargeType: ChargeType,
    val description: String,
    val quantity: Int,
    val unitPrice: BigDecimal,
    val totalAmount: BigDecimal,
)
