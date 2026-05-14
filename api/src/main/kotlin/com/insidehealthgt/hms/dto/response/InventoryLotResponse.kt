package com.insidehealthgt.hms.dto.response

import com.insidehealthgt.hms.entity.InventoryLot
import java.time.LocalDate
import java.time.LocalDateTime

data class InventoryLotResponse(
    val id: Long,
    val itemId: Long,
    val itemName: String?,
    val itemSku: String?,
    val lotNumber: String?,
    val expirationDate: LocalDate,
    val quantityOnHand: Int,
    val receivedAt: LocalDate,
    val supplier: String?,
    val notes: String?,
    val recalled: Boolean,
    val recalledReason: String?,
    val syntheticLegacy: Boolean,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?,
) {
    companion object {
        fun from(lot: InventoryLot): InventoryLotResponse = InventoryLotResponse(
            id = lot.id!!,
            itemId = lot.item.id!!,
            itemName = lot.item.name,
            itemSku = lot.item.sku,
            lotNumber = lot.lotNumber,
            expirationDate = lot.expirationDate,
            quantityOnHand = lot.quantityOnHand,
            receivedAt = lot.receivedAt,
            supplier = lot.supplier,
            notes = lot.notes,
            recalled = lot.recalled,
            recalledReason = lot.recalledReason,
            syntheticLegacy = lot.syntheticLegacy,
            createdAt = lot.createdAt,
            updatedAt = lot.updatedAt,
        )
    }
}
