package com.insidehealthgt.hms.service

import com.insidehealthgt.hms.dto.response.ExpiryReportResponse
import com.insidehealthgt.hms.dto.response.ExpiryReportRow
import com.insidehealthgt.hms.dto.response.LotExpiryStatus
import com.insidehealthgt.hms.entity.MedicationSection
import com.insidehealthgt.hms.repository.InventoryLotRepository
import com.insidehealthgt.hms.repository.MedicationDetailsRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

@Service
class ExpiryReportService(
    private val lotRepository: InventoryLotRepository,
    private val detailsRepository: MedicationDetailsRepository,
) {

    @Transactional(readOnly = true)
    fun build(window: Int, urgentWindow: Int, section: MedicationSection?, controlled: Boolean?): ExpiryReportResponse {
        val today = LocalDate.now()
        val lots = lotRepository.findAllActiveWithItem()

        // Pre-fetch details per item.
        val itemIds = lots.map { it.item.id!! }.toSet()
        val detailsByItem = itemIds.mapNotNull { id -> detailsRepository.findByItemId(id) }
            .associateBy { it.item.id!! }

        val rows = lots.mapNotNull { lot ->
            val details = detailsByItem[lot.item.id]
            if (section != null && details?.section != section) return@mapNotNull null
            if (controlled != null && details?.controlled != controlled) return@mapNotNull null
            val days = if (lot.expirationDate == PharmacyConstants.LEGACY_LOT_EXPIRATION) {
                null
            } else {
                ChronoUnit.DAYS.between(today, lot.expirationDate)
            }
            val status = classify(lot.expirationDate, today, window, urgentWindow)
            ExpiryReportRow(
                lotId = lot.id!!,
                itemId = lot.item.id!!,
                sku = lot.item.sku,
                genericName = details?.genericName ?: lot.item.name,
                commercialName = details?.commercialName,
                strength = details?.strength,
                section = details?.section,
                lotNumber = lot.lotNumber,
                expirationDate = lot.expirationDate,
                daysToExpiry = days,
                status = status,
                quantityOnHand = lot.quantityOnHand,
                recalled = lot.recalled,
            )
        }.sortedWith(compareBy({ it.status.ordinal }, { it.expirationDate }))

        val totals = LotExpiryStatus.values().associateWith { s -> rows.count { it.status == s } }
        return ExpiryReportResponse(LocalDateTime.now(), totals, rows)
    }

    private fun classify(exp: LocalDate, today: LocalDate, window: Int, urgentWindow: Int): LotExpiryStatus {
        val days = ChronoUnit.DAYS.between(today, exp)
        return when {
            exp == PharmacyConstants.LEGACY_LOT_EXPIRATION -> LotExpiryStatus.NO_EXPIRY
            exp.isBefore(today) -> LotExpiryStatus.EXPIRED
            days <= urgentWindow -> LotExpiryStatus.RED
            days <= window -> LotExpiryStatus.YELLOW
            else -> LotExpiryStatus.GREEN
        }
    }
}
