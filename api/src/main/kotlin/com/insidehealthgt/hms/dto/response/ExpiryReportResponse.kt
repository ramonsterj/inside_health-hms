package com.insidehealthgt.hms.dto.response

import com.insidehealthgt.hms.entity.MedicationSection
import java.time.LocalDate
import java.time.LocalDateTime

enum class LotExpiryStatus {
    EXPIRED,
    RED,
    YELLOW,
    GREEN,
    NO_EXPIRY,
}

data class ExpiryReportRow(
    val lotId: Long,
    val itemId: Long,
    val sku: String?,
    val genericName: String?,
    val commercialName: String?,
    val strength: String?,
    val section: MedicationSection?,
    val lotNumber: String?,
    val expirationDate: LocalDate,
    val daysToExpiry: Long?,
    val status: LotExpiryStatus,
    val quantityOnHand: Int,
    val recalled: Boolean,
)

data class ExpiryReportResponse(
    val generatedAt: LocalDateTime,
    val totals: Map<LotExpiryStatus, Int>,
    val items: List<ExpiryReportRow>,
)
