package com.insidehealthgt.hms.dto.response

import com.insidehealthgt.hms.entity.InventoryKind
import com.insidehealthgt.hms.repository.InventoryWarehouseStockRepository
import java.math.BigDecimal

/** One row per item in a warehouse's stock view (lot rows collapse into a sum). */
data class WarehouseStockResponse(
    val itemId: Long,
    val name: String,
    val sku: String?,
    val kind: InventoryKind,
    val price: BigDecimal,
    val restockLevel: Int,
    val quantity: Int,
    val lowStock: Boolean,
) {
    companion object {
        fun from(row: InventoryWarehouseStockRepository.WarehouseStockRow): WarehouseStockResponse {
            val qty = row.quantity.toInt()
            return WarehouseStockResponse(
                itemId = row.itemId,
                name = row.name,
                sku = row.sku,
                kind = InventoryKind.valueOf(row.kind),
                price = row.price,
                restockLevel = row.restockLevel,
                quantity = qty,
                lowStock = row.restockLevel > 0 && qty <= row.restockLevel,
            )
        }
    }
}
