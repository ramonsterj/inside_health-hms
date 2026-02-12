package com.insidehealthgt.hms.dto.response

import com.insidehealthgt.hms.entity.ChargeType
import com.insidehealthgt.hms.entity.PatientCharge
import com.insidehealthgt.hms.entity.User
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

data class PatientChargeResponse(
    val id: Long,
    val admissionId: Long,
    val chargeType: ChargeType,
    val description: String,
    val quantity: Int,
    val unitPrice: BigDecimal,
    val totalAmount: BigDecimal,
    val inventoryItemName: String?,
    val roomNumber: String?,
    val invoiced: Boolean,
    val reason: String?,
    val chargeDate: LocalDate,
    val createdAt: LocalDateTime?,
    val createdByName: String?,
) {
    companion object {
        fun from(charge: PatientCharge, createdByUser: User? = null) = PatientChargeResponse(
            id = charge.id!!,
            admissionId = charge.admission.id!!,
            chargeType = charge.chargeType,
            description = charge.description,
            quantity = charge.quantity,
            unitPrice = charge.unitPrice,
            totalAmount = charge.totalAmount,
            inventoryItemName = charge.inventoryItem?.name,
            roomNumber = charge.room?.number,
            invoiced = charge.invoice != null,
            reason = charge.reason,
            chargeDate = charge.chargeDate,
            createdAt = charge.createdAt,
            createdByName = createdByUser?.let { "${it.firstName ?: ""} ${it.lastName ?: ""}".trim() },
        )
    }
}
