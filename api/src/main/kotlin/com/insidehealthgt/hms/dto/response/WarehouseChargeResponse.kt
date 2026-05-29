package com.insidehealthgt.hms.dto.response

import com.insidehealthgt.hms.entity.User
import com.insidehealthgt.hms.entity.WarehouseCharge
import java.math.BigDecimal
import java.time.LocalDateTime

data class WarehouseChargeResponse(
    val id: Long,
    val warehouse: WarehouseResponse.Summary,
    val item: TransferResponse.ItemSummary,
    val admission: AdmissionSummary,
    val quantity: Int,
    val amount: BigDecimal,
    val reason: String,
    val notes: String?,
    val chargeId: Long?,
    val createdBy: UserSummaryResponse?,
    val createdAt: LocalDateTime?,
) {
    data class AdmissionSummary(val id: Long, val patientName: String, val roomNumber: String?)

    companion object {
        fun from(charge: WarehouseCharge, createdByUser: User? = null): WarehouseChargeResponse {
            val patient = charge.admission.patient
            return WarehouseChargeResponse(
                id = charge.id!!,
                warehouse = WarehouseResponse.Summary.from(charge.warehouse),
                item = TransferResponse.ItemSummary(charge.item.id!!, charge.item.name, charge.item.sku),
                admission = AdmissionSummary(
                    id = charge.admission.id!!,
                    patientName = "${patient.firstName} ${patient.lastName}",
                    roomNumber = charge.admission.room?.number,
                ),
                quantity = charge.quantity.toInt(),
                amount = charge.amount,
                reason = charge.reason,
                notes = charge.notes,
                chargeId = charge.charge?.id,
                createdBy = createdByUser?.let { UserSummaryResponse.from(it) },
                createdAt = charge.createdAt,
            )
        }
    }
}
