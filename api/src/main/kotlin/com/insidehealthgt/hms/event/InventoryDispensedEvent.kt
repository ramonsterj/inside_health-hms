package com.insidehealthgt.hms.event

import java.math.BigDecimal

data class InventoryDispensedEvent(
    val admissionId: Long,
    val inventoryItemId: Long,
    val itemName: String,
    val quantity: Int,
    val unitPrice: BigDecimal,
)
