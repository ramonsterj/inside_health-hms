package com.insidehealthgt.hms.service

import com.insidehealthgt.hms.dto.response.ExpiryReportResponse
import com.insidehealthgt.hms.dto.response.ExpiryReportRow
import com.insidehealthgt.hms.dto.response.LotExpiryStatus
import com.insidehealthgt.hms.entity.MedicationSection
import com.insidehealthgt.hms.repository.InventoryLotRepository
import com.insidehealthgt.hms.repository.InventoryWarehouseStockRepository
import com.insidehealthgt.hms.repository.MedicationDetailsRepository
import com.insidehealthgt.hms.security.CurrentUserProvider
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

@Service
class ExpiryReportService(
    private val lotRepository: InventoryLotRepository,
    private val stockRepository: InventoryWarehouseStockRepository,
    private val detailsRepository: MedicationDetailsRepository,
    private val scopeService: WarehouseScopeService,
    private val currentUserProvider: CurrentUserProvider,
) {

    @Transactional(readOnly = true)
    @Suppress("LongParameterList")
    fun build(
        window: Int,
        urgentWindow: Int,
        section: MedicationSection?,
        controlled: Boolean?,
        warehouseId: Long? = null,
    ): ExpiryReportResponse {
        val today = LocalDate.now()
        val lots = lotRepository.findAllActiveWithItem()

        // Per-lot on-hand: scoped to a warehouse (FR-8/AC-15) or summed system-wide.
        val quantityByLot: Map<Long, Int> = if (warehouseId != null) {
            // A warehouse-scoped caller (e.g. JEFE_ENFERMERIA) cannot inspect another
            // bodega's stock by passing its id (AC-13 parity with the stock view).
            scopeService.assertCanView(currentUserProvider.currentUserDetailsOrThrow(), warehouseId)
            stockRepository.findLotStockInWarehouse(warehouseId).associate { it.lotId to it.quantity.toInt() }
        } else {
            stockRepository.sumByLotIds(lots.mapNotNull { it.id }).associate { it.lotId to it.quantity.toInt() }
        }

        // Pre-fetch details per item.
        val itemIds = lots.map { it.item.id!! }.toSet()
        val detailsByItem = itemIds.mapNotNull { id -> detailsRepository.findByItemId(id) }
            .associateBy { it.item.id!! }

        val rows = lots.mapNotNull { lot ->
            // When filtered to a warehouse, only lots with positive stock there show.
            if (warehouseId != null && (quantityByLot[lot.id] ?: 0) <= 0) return@mapNotNull null
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
                quantityOnHand = quantityByLot[lot.id] ?: 0,
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
