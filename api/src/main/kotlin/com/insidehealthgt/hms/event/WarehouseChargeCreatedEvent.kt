package com.insidehealthgt.hms.event

import java.math.BigDecimal

/**
 * Published after a non-medical consumable is charged from a warehouse to an
 * admission. The billing module listens (AFTER_COMMIT / REQUIRES_NEW) and creates
 * the [com.insidehealthgt.hms.entity.PatientCharge], then the warehouse-charge row
 * is linked back to it. See [com.insidehealthgt.hms.service.WarehouseChargeService].
 */
data class WarehouseChargeCreatedEvent(
    val warehouseChargeId: Long,
    val admissionId: Long,
    val inventoryItemId: Long,
    val itemName: String,
    val quantity: Int,
    val unitPrice: BigDecimal,
    val reason: String,
)
