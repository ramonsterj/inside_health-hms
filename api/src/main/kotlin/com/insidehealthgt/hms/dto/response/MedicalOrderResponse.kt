package com.insidehealthgt.hms.dto.response

import com.insidehealthgt.hms.entity.AdministrationRoute
import com.insidehealthgt.hms.entity.EmergencyAuthorizationReason
import com.insidehealthgt.hms.entity.MedicalOrder
import com.insidehealthgt.hms.entity.MedicalOrderCategory
import com.insidehealthgt.hms.entity.MedicalOrderStatus
import com.insidehealthgt.hms.entity.User
import java.time.LocalDate
import java.time.LocalDateTime

@Suppress("LongParameterList")
data class MedicalOrderResponse(
    val id: Long,
    val admissionId: Long,
    val category: MedicalOrderCategory,
    val startDate: LocalDate,
    val endDate: LocalDate?,
    val medication: String?,
    val dosage: String?,
    val route: AdministrationRoute?,
    val frequency: String?,
    val schedule: String?,
    val observations: String?,
    val status: MedicalOrderStatus,
    val authorizedAt: LocalDateTime?,
    val authorizedBy: MedicalStaffResponse?,
    val inProgressAt: LocalDateTime?,
    val inProgressBy: MedicalStaffResponse?,
    val resultsReceivedAt: LocalDateTime?,
    val resultsReceivedBy: MedicalStaffResponse?,
    val rejectedAt: LocalDateTime?,
    val rejectedBy: MedicalStaffResponse?,
    val rejectionReason: String?,
    val emergencyAuthorized: Boolean,
    val emergencyReason: EmergencyAuthorizationReason?,
    val emergencyReasonNote: String?,
    val emergencyAt: LocalDateTime?,
    val emergencyBy: MedicalStaffResponse?,
    val discontinuedAt: LocalDateTime?,
    val discontinuedBy: MedicalStaffResponse?,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?,
    val createdBy: MedicalStaffResponse?,
    val updatedBy: MedicalStaffResponse?,
    val inventoryItemId: Long?,
    val inventoryItemName: String?,
    val documentCount: Int = 0,
) {
    companion object {
        @Suppress("LongParameterList")
        fun from(
            medicalOrder: MedicalOrder,
            createdByUser: User? = null,
            updatedByUser: User? = null,
            discontinuedByUser: User? = null,
            authorizedByUser: User? = null,
            rejectedByUser: User? = null,
            inProgressByUser: User? = null,
            resultsReceivedByUser: User? = null,
            emergencyByUser: User? = null,
            documentCount: Int = 0,
        ): MedicalOrderResponse = MedicalOrderResponse(
            id = medicalOrder.id!!,
            admissionId = medicalOrder.admission.id!!,
            category = medicalOrder.category,
            startDate = medicalOrder.startDate,
            endDate = medicalOrder.endDate,
            medication = medicalOrder.medication,
            dosage = medicalOrder.dosage,
            route = medicalOrder.route,
            frequency = medicalOrder.frequency,
            schedule = medicalOrder.schedule,
            observations = medicalOrder.observations,
            status = medicalOrder.status,
            authorizedAt = medicalOrder.authorizedAt,
            authorizedBy = authorizedByUser.toStaff(),
            inProgressAt = medicalOrder.inProgressAt,
            inProgressBy = inProgressByUser.toStaff(),
            resultsReceivedAt = medicalOrder.resultsReceivedAt,
            resultsReceivedBy = resultsReceivedByUser.toStaff(),
            rejectedAt = medicalOrder.rejectedAt,
            rejectedBy = rejectedByUser.toStaff(),
            rejectionReason = medicalOrder.rejectionReason,
            emergencyAuthorized = medicalOrder.emergencyAuthorized,
            emergencyReason = medicalOrder.emergencyReason,
            emergencyReasonNote = medicalOrder.emergencyReasonNote,
            emergencyAt = medicalOrder.emergencyAt,
            emergencyBy = emergencyByUser.toStaff(),
            discontinuedAt = medicalOrder.discontinuedAt,
            discontinuedBy = discontinuedByUser.toStaff(),
            createdAt = medicalOrder.createdAt,
            updatedAt = medicalOrder.updatedAt,
            createdBy = createdByUser.toStaff(),
            updatedBy = updatedByUser.toStaff(),
            inventoryItemId = medicalOrder.inventoryItem?.id,
            inventoryItemName = medicalOrder.inventoryItem?.name,
            documentCount = documentCount,
        )

        private fun User?.toStaff(): MedicalStaffResponse? = this?.let { MedicalStaffResponse.from(it) }
    }
}
