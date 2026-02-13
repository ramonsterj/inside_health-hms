package com.insidehealthgt.hms.event

import com.insidehealthgt.hms.entity.MedicalOrderCategory
import java.math.BigDecimal

data class MedicalOrderCreatedEvent(
    val admissionId: Long,
    val category: MedicalOrderCategory,
    val inventoryItemId: Long,
    val itemName: String,
    val unitPrice: BigDecimal,
)
