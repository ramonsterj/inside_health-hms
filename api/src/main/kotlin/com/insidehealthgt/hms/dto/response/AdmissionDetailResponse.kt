package com.insidehealthgt.hms.dto.response

import com.insidehealthgt.hms.entity.Admission
import com.insidehealthgt.hms.entity.AdmissionStatus
import com.insidehealthgt.hms.entity.AdmissionType
import com.insidehealthgt.hms.entity.User
import java.time.LocalDateTime

data class AdmissionDetailResponse(
    val id: Long,
    val patient: PatientSummaryResponse,
    val triageCode: TriageCodeSummaryResponse?,
    val room: RoomSummaryResponse?,
    val treatingPhysician: DoctorResponse,
    val admissionDate: LocalDateTime,
    val dischargeDate: LocalDateTime?,
    val status: AdmissionStatus,
    val type: AdmissionType,
    val inventory: String?,
    val hasConsentDocument: Boolean,
    val consultingPhysicians: List<ConsultingPhysicianResponse>,
    val createdAt: LocalDateTime?,
    val createdBy: UserSummaryResponse?,
    val updatedAt: LocalDateTime?,
    val updatedBy: UserSummaryResponse?,
) {
    companion object {
        fun from(
            admission: Admission,
            createdByUser: User? = null,
            updatedByUser: User? = null,
            consultingPhysicianCreatedByUsers: Map<Long, User> = emptyMap(),
        ): AdmissionDetailResponse = AdmissionDetailResponse(
            id = admission.id!!,
            patient = PatientSummaryResponse.from(admission.patient),
            triageCode = admission.triageCode?.let { TriageCodeSummaryResponse.from(it) },
            room = admission.room?.let { RoomSummaryResponse.from(it) },
            treatingPhysician = DoctorResponse.from(admission.treatingPhysician),
            admissionDate = admission.admissionDate,
            dischargeDate = admission.dischargeDate,
            status = admission.status,
            type = admission.type,
            inventory = admission.inventory,
            hasConsentDocument = admission.hasConsentDocument(),
            consultingPhysicians = admission.consultingPhysicians.map { cp ->
                ConsultingPhysicianResponse.from(cp, cp.createdBy?.let { consultingPhysicianCreatedByUsers[it] })
            },
            createdAt = admission.createdAt,
            createdBy = createdByUser?.let { UserSummaryResponse.from(it) },
            updatedAt = admission.updatedAt,
            updatedBy = updatedByUser?.let { UserSummaryResponse.from(it) },
        )
    }
}
