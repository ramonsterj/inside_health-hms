package com.insidehealthgt.hms.dto.response

import com.insidehealthgt.hms.entity.AdministrationRoute
import com.insidehealthgt.hms.entity.DosageForm
import com.insidehealthgt.hms.entity.InventoryItem
import com.insidehealthgt.hms.entity.MedicationDetails
import com.insidehealthgt.hms.entity.MedicationReviewStatus
import com.insidehealthgt.hms.entity.MedicationSection
import java.math.BigDecimal

data class MedicationResponse(
    val id: Long,
    val itemId: Long,
    val name: String,
    val description: String?,
    val sku: String?,
    val categoryId: Long,
    val price: BigDecimal,
    val cost: BigDecimal,
    val restockLevel: Int,
    val quantity: Int,
    val active: Boolean,
    val genericName: String,
    val commercialName: String?,
    val strength: String?,
    val dosageForm: DosageForm,
    val route: AdministrationRoute?,
    val controlled: Boolean,
    val atcCode: String?,
    val section: MedicationSection,
    val reviewStatus: MedicationReviewStatus,
    val reviewNotes: String?,
    /**
     * Per-warehouse on-hand breakdown (FR-11). `quantity` above is the system-wide
     * total; this array makes explicit where that stock actually sits, since
     * dispensing is warehouse-scoped. Empty on the list endpoint (detail-only).
     */
    val warehouseStock: List<WarehouseStockBreakdown> = emptyList(),
) {
    data class WarehouseStockBreakdown(
        val warehouseId: Long,
        val warehouseCode: String,
        val warehouseName: String,
        val quantity: Int,
    )

    companion object {
        fun from(
            item: InventoryItem,
            details: MedicationDetails,
            quantity: Int,
            warehouseStock: List<WarehouseStockBreakdown> = emptyList(),
        ): MedicationResponse = MedicationResponse(
            id = details.id!!,
            itemId = item.id!!,
            name = item.name,
            description = item.description,
            sku = item.sku,
            categoryId = item.category.id!!,
            price = item.price,
            cost = item.cost,
            restockLevel = item.restockLevel,
            quantity = quantity,
            active = item.active,
            genericName = details.genericName,
            commercialName = details.commercialName,
            strength = details.strength,
            dosageForm = details.dosageForm,
            route = details.route,
            controlled = details.controlled,
            atcCode = details.atcCode,
            section = details.section,
            reviewStatus = details.reviewStatus,
            reviewNotes = details.reviewNotes,
            warehouseStock = warehouseStock,
        )
    }
}
