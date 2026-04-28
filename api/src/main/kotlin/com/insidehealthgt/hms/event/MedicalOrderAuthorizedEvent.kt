package com.insidehealthgt.hms.event

import com.insidehealthgt.hms.entity.MedicalOrderCategory
import java.math.BigDecimal

/**
 * Published when a medical order with a billable category and a linked inventory item
 * is authorized. Triggers billing-charge creation. Rejected orders never produce a charge
 * because authorization is the only state that publishes this event.
 */
data class MedicalOrderAuthorizedEvent(
    val admissionId: Long,
    val category: MedicalOrderCategory,
    val inventoryItemId: Long,
    val itemName: String,
    val unitPrice: BigDecimal,
)
