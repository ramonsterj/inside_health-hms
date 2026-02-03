package com.insidehealthgt.hms.dto.response

import com.insidehealthgt.hms.entity.Admission
import com.insidehealthgt.hms.entity.AdmissionStatus
import com.insidehealthgt.hms.entity.AdmissionType
import java.time.LocalDateTime

data class AdmissionListResponse(
    val id: Long,
    val patient: PatientSummaryResponse,
    val triageCode: TriageCodeSummaryResponse?,
    val room: RoomSummaryResponse?,
    val treatingPhysician: DoctorResponse,
    val admissionDate: LocalDateTime,
    val dischargeDate: LocalDateTime?,
    val status: AdmissionStatus,
    val type: AdmissionType,
    val hasConsentDocument: Boolean,
    val createdAt: LocalDateTime?,
) {
    companion object {
        fun from(admission: Admission): AdmissionListResponse = AdmissionListResponse(
            id = admission.id!!,
            patient = PatientSummaryResponse.from(admission.patient),
            triageCode = admission.triageCode?.let { TriageCodeSummaryResponse.from(it) },
            room = admission.room?.let { RoomSummaryResponse.from(it) },
            treatingPhysician = DoctorResponse.from(admission.treatingPhysician),
            admissionDate = admission.admissionDate,
            dischargeDate = admission.dischargeDate,
            status = admission.status,
            type = admission.type,
            hasConsentDocument = admission.hasConsentDocument(),
            createdAt = admission.createdAt,
        )
    }
}

data class TriageCodeSummaryResponse(val id: Long, val code: String, val color: String, val description: String?) {
    companion object {
        fun from(triageCode: com.insidehealthgt.hms.entity.TriageCode) = TriageCodeSummaryResponse(
            id = triageCode.id!!,
            code = triageCode.code,
            color = triageCode.color,
            description = triageCode.description,
        )
    }
}

data class RoomSummaryResponse(val id: Long, val number: String, val type: com.insidehealthgt.hms.entity.RoomType) {
    companion object {
        fun from(room: com.insidehealthgt.hms.entity.Room) = RoomSummaryResponse(
            id = room.id!!,
            number = room.number,
            type = room.type,
        )
    }
}
