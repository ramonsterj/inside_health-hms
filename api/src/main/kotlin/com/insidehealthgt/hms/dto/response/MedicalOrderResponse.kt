package com.insidehealthgt.hms.dto.response

import com.insidehealthgt.hms.entity.AdministrationRoute
import com.insidehealthgt.hms.entity.MedicalOrder
import com.insidehealthgt.hms.entity.MedicalOrderCategory
import com.insidehealthgt.hms.entity.MedicalOrderStatus
import com.insidehealthgt.hms.entity.User
import java.time.LocalDate
import java.time.LocalDateTime

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
    val discontinuedAt: LocalDateTime?,
    val discontinuedBy: MedicalStaffResponse?,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?,
    val createdBy: MedicalStaffResponse?,
    val updatedBy: MedicalStaffResponse?,
) {
    companion object {
        fun from(
            medicalOrder: MedicalOrder,
            createdByUser: User? = null,
            updatedByUser: User? = null,
            discontinuedByUser: User? = null,
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
            discontinuedAt = medicalOrder.discontinuedAt,
            discontinuedBy = discontinuedByUser?.let { MedicalStaffResponse.from(it) },
            createdAt = medicalOrder.createdAt,
            updatedAt = medicalOrder.updatedAt,
            createdBy = createdByUser?.let { MedicalStaffResponse.from(it) },
            updatedBy = updatedByUser?.let { MedicalStaffResponse.from(it) },
        )
    }
}
