package com.insidehealthgt.hms.dto.response

import com.insidehealthgt.hms.entity.AdmissionConsultingPhysician
import com.insidehealthgt.hms.entity.User
import java.time.LocalDate
import java.time.LocalDateTime

data class ConsultingPhysicianResponse(
    val id: Long,
    val physician: DoctorResponse,
    val reason: String?,
    val requestedDate: LocalDate?,
    val createdAt: LocalDateTime?,
    val createdBy: UserSummaryResponse?,
) {
    companion object {
        fun from(consultingPhysician: AdmissionConsultingPhysician, createdByUser: User? = null) =
            ConsultingPhysicianResponse(
                id = consultingPhysician.id!!,
                physician = DoctorResponse.from(consultingPhysician.physician),
                reason = consultingPhysician.reason,
                requestedDate = consultingPhysician.requestedDate,
                createdAt = consultingPhysician.createdAt,
                createdBy = createdByUser?.let { UserSummaryResponse.from(it) },
            )
    }
}
