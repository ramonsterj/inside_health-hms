package com.insidehealthgt.hms.dto.response

import com.insidehealthgt.hms.entity.AdministrationRoute
import com.insidehealthgt.hms.entity.AdministrationStatus
import com.insidehealthgt.hms.entity.MedicationAdministration
import com.insidehealthgt.hms.entity.User
import java.time.LocalDateTime

data class MedicationAdministrationResponse(
    val id: Long,
    val medicalOrderId: Long,
    val admissionId: Long,
    val medication: String?,
    val dosage: String?,
    val route: AdministrationRoute?,
    val status: AdministrationStatus,
    val notes: String?,
    val administeredAt: LocalDateTime,
    val administeredByName: String?,
    val inventoryItemName: String?,
    val billable: Boolean,
) {
    companion object {
        fun from(
            administration: MedicationAdministration,
            administeredByUser: User? = null,
            billable: Boolean = false,
        ): MedicationAdministrationResponse {
            val order = administration.medicalOrder
            return MedicationAdministrationResponse(
                id = administration.id!!,
                medicalOrderId = order.id!!,
                admissionId = administration.admission.id!!,
                medication = order.medication,
                dosage = order.dosage,
                route = order.route,
                status = administration.status,
                notes = administration.notes,
                administeredAt = administration.administeredAt,
                administeredByName = administeredByUser?.let {
                    listOfNotNull(it.firstName, it.lastName).joinToString(" ")
                },
                inventoryItemName = order.inventoryItem?.name,
                billable = billable,
            )
        }
    }
}
