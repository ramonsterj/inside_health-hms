package com.insidehealthgt.hms.dto.response

import com.insidehealthgt.hms.entity.InventoryTransfer
import com.insidehealthgt.hms.entity.TransferStatus
import com.insidehealthgt.hms.entity.User
import java.time.LocalDate
import java.time.LocalDateTime

data class TransferResponse(
    val id: Long,
    val status: TransferStatus,
    val sourceWarehouse: WarehouseResponse.Summary,
    val destinationWarehouse: WarehouseResponse.Summary,
    val item: ItemSummary,
    val lot: LotSummary?,
    val quantity: Int,
    val notes: String?,
    val issuedBy: UserSummaryResponse?,
    val issuedAt: LocalDateTime,
    val completedAt: LocalDateTime?,
) {
    data class ItemSummary(val id: Long, val name: String, val sku: String?)
    data class LotSummary(val id: Long, val lotNumber: String?, val expirationDate: LocalDate)

    companion object {
        fun from(transfer: InventoryTransfer, issuedByUser: User? = null): TransferResponse = TransferResponse(
            id = transfer.id!!,
            status = transfer.status,
            sourceWarehouse = WarehouseResponse.Summary.from(transfer.sourceWarehouse),
            destinationWarehouse = WarehouseResponse.Summary.from(transfer.destinationWarehouse),
            item = ItemSummary(transfer.item.id!!, transfer.item.name, transfer.item.sku),
            lot = transfer.lot?.let { LotSummary(it.id!!, it.lotNumber, it.expirationDate) },
            quantity = transfer.quantity.toInt(),
            notes = transfer.notes,
            issuedBy = issuedByUser?.let { UserSummaryResponse.from(it) },
            issuedAt = transfer.issuedAt,
            completedAt = transfer.completedAt,
        )
    }
}
