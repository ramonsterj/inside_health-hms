package com.insidehealthgt.hms.dto.response

import com.insidehealthgt.hms.entity.MedicalOrder
import com.insidehealthgt.hms.entity.MedicalOrderCategory
import com.insidehealthgt.hms.entity.MedicalOrderStatus
import com.insidehealthgt.hms.entity.User
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Lightweight projection used by the cross-admission orders-by-state dashboard.
 * Carries patient + admission summary fields so the UI can render the row without
 * a second roundtrip per order.
 */
data class MedicalOrderListItemResponse(
    val id: Long,
    val admissionId: Long,
    val patientId: Long,
    val patientFirstName: String,
    val patientLastName: String,
    val category: MedicalOrderCategory,
    val status: MedicalOrderStatus,
    val startDate: LocalDate,
    val summary: String?,
    val medication: String?,
    val dosage: String?,
    val createdAt: LocalDateTime?,
    val createdBy: MedicalStaffResponse?,
    val authorizedAt: LocalDateTime?,
    val rejectedAt: LocalDateTime?,
    val inProgressAt: LocalDateTime?,
    val resultsReceivedAt: LocalDateTime?,
    val discontinuedAt: LocalDateTime?,
    val emergencyAuthorized: Boolean,
    val documentCount: Int,
) {
    companion object {
        fun from(order: MedicalOrder, createdByUser: User?, documentCount: Int): MedicalOrderListItemResponse =
            MedicalOrderListItemResponse(
                id = order.id!!,
                admissionId = order.admission.id!!,
                patientId = order.admission.patient.id!!,
                patientFirstName = order.admission.patient.firstName,
                patientLastName = order.admission.patient.lastName,
                category = order.category,
                status = order.status,
                startDate = order.startDate,
                summary = order.observations?.take(SUMMARY_MAX_LENGTH),
                medication = order.medication,
                dosage = order.dosage,
                createdAt = order.createdAt,
                createdBy = createdByUser?.let { MedicalStaffResponse.from(it) },
                authorizedAt = order.authorizedAt,
                rejectedAt = order.rejectedAt,
                inProgressAt = order.inProgressAt,
                resultsReceivedAt = order.resultsReceivedAt,
                discontinuedAt = order.discontinuedAt,
                emergencyAuthorized = order.emergencyAuthorized,
                documentCount = documentCount,
            )

        private const val SUMMARY_MAX_LENGTH = 200
    }
}
